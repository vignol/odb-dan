package pack;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class Parser6 {

    private static final Map<String,String> typeTranslation = new HashMap<String,String>() {{
        put("[B","Lpack/Pair;");
        put("Ljava/net/Socket;","Lodb/MySocket;");
        put("Ljava/net/ServerSocket;","Lodb/MyServerSocket;");
        put("Ljava/io/OutputStream;","Lodb/MyOutputStream;");
        put("Ljava/io/InputStream;","Lodb/MyInputStream;");
        put("Ljava/io/FileInputStream;","Lodb/MyFileInputStream;");
        put("Ljakarta/servlet/ServletInputStream;","Lodb/MyServletInputStream;");
        put("Ljakarta/servlet/ServletOutputStream;","Lodb/MyServletOutputStream;");
        put("Ljakarta/servlet/http/HttpServletRequest;","Lodb/MyHttpServletRequest;");
        put("Ljakarta/servlet/http/HttpServletResponse;","Lodb/MyHttpServletResponse;");

    }};

    // only for NEW and method owners
    private static final Map<String,String> classTranslation = new HashMap<String,String>() {{
        put("java/net/Socket","odb/MySocket");
        put("java/net/ServerSocket","odb/MyServerSocket");
        put("java/io/OutputStream","odb/MyOutputStream");
        put("java/io/InputStream","odb/MyInputStream");  
        put("java/io/FileInputStream","odb/MyFileInputStream");
        put("jakarta/servlet/ServletInputStream","odb/MyServletInputStream");
        put("jakarta/servlet/ServletOutputStream","odb/MyServletOutputStream");
        put("jakarta/servlet/http/HttpServletRequest","odb/MyHttpServletRequest");
        put("jakarta/servlet/http/HttpServletResponse","odb/MyHttpServletResponse");

    }};


    //////////////////////////////////////////
    /// replace each type (to adapt) in each field
    /// rely on the typeTranslation table above
    static List<FieldNode> parseFields(List<FieldNode> fields) {
        List<FieldNode> lf = new ArrayList<FieldNode>();
        for (FieldNode f : fields) {
            f.signature = null; // Clear generics signature as types are changing
            String desttype = typeTranslation.get(f.desc);
            if (desttype != null) {
                lf.add(new FieldNode(f.access, f.name, desttype, null, null));
            } else
                lf.add(f);
        }
        return lf;
    }

    //////////////////////////////////////////
    /// replace each type (to adapt) in parameters of a method signature
    /// trace : whether we should print traces of the adapter parameters
    /// rely on the typeTranslation table above

static String parseMethodDesc(String name, String desc, boolean trace) {
    if (name.equals("doGet") || name.equals("doPost")) {
        return desc;  // NE PAS transformer la signature
    }
    String desttype;
    Method meth = new Method(name, desc);
    Type ret = meth.getReturnType();
    desttype = typeTranslation.get(ret.getDescriptor());
    if (desttype != null) {
        ret = Type.getType(desttype);
    }
    int i=0;
    Type[] args = meth.getArgumentTypes();
    ArrayList<Type> newargs = new ArrayList<Type>();
    for (Type t : args) {
        desttype = typeTranslation.get(t.getDescriptor());
        if (desttype != null) {
            newargs.add(Type.getType(desttype));
        } else
            newargs.add(t);
        i++;
    }
    meth = new Method(name,ret,newargs.toArray(new Type[0]));
    return meth.getDescriptor();
}

    //////////////////////////////////////////
    /// replace each type (to adapt) in local variables of a method
    /// rely on the typeTranslation table above
    
static void parseLocalVariables(MethodNode m) {
    if (m.localVariables == null) {
        return;
    }
    List<LocalVariableNode> list = m.localVariables;
    List<LocalVariableNode> newlist = new ArrayList<LocalVariableNode>();
    for (LocalVariableNode lv : list) {
        String desttype = typeTranslation.get(lv.desc);
        if (desttype != null) {
            newlist.add(new LocalVariableNode(lv.name, desttype, null, lv.start, lv.end, lv.index));
        } else {
            // For doGet/doPost, update the type of the original request and response variables
            if ((m.name.equals("doGet") || m.name.equals("doPost"))) {
                if (lv.index == 1) { // request
                    newlist.add(new LocalVariableNode(lv.name, "Lodb/MyHttpServletRequest;", null, lv.start, lv.end, lv.index));
                    continue;
                }
                if (lv.index == 2) { // response
                    newlist.add(new LocalVariableNode(lv.name, "Lodb/MyHttpServletResponse;", null, lv.start, lv.end, lv.index));
                    continue;
                }
            }
             newlist.add(lv);
        }
    }
    m.localVariables = newlist;
}

    //////////////////////////////////////////
    /// parse all the instructions from a method, replacing instructions
    
static void parseInstructions(MethodNode m) {
    InstructionStack stack = new InstructionStack();
    Set<AbstractInsnNode> visited = new HashSet<AbstractInsnNode>();
    InsnList instructions = m.instructions;
    if (instructions.size() == 0) return;
    
    // If method is doGet or doPost, insert wrappers and replace var loads
    if (m.name.equals("doGet") || m.name.equals("doPost")) {
        m.maxStack += 4; // Increase maxStack for wrapper construction

        // Get the first instruction of the original method
        AbstractInsnNode originalFirstInsn = instructions.getFirst();

        // Insert wrapper code at the beginning FIRST
        InsnList insert = new InsnList();
        
        // new MyHttpServletRequest(request)
        insert.add(new TypeInsnNode(Opcodes.NEW, "odb/MyHttpServletRequest"));
        insert.add(new InsnNode(Opcodes.DUP));
        insert.add(new VarInsnNode(Opcodes.ALOAD, 1)); // load original request
        insert.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "odb/MyHttpServletRequest", "<init>", "(Ljakarta/servlet/http/HttpServletRequest;)V", false));
        // Overwrite the original 'request' variable in slot 1
        insert.add(new VarInsnNode(Opcodes.ASTORE, 1));

        // new MyHttpServletResponse(response, request)
        insert.add(new TypeInsnNode(Opcodes.NEW, "odb/MyHttpServletResponse"));
        insert.add(new InsnNode(Opcodes.DUP));
        insert.add(new VarInsnNode(Opcodes.ALOAD, 2)); // load original response
        insert.add(new VarInsnNode(Opcodes.ALOAD, 1)); // load wrapped request
        insert.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "odb/MyHttpServletResponse", "<init>", "(Ljakarta/servlet/http/HttpServletResponse;Lodb/MyHttpServletRequest;)V", false));
        // Overwrite the original 'response' variable in slot 2
        insert.add(new VarInsnNode(Opcodes.ASTORE, 2));

        instructions.insertBefore(originalFirstInsn, insert);
    }

    AbstractInsnNode firstinst = instructions.getFirst();
    parseBranch(m, firstinst, visited, stack);
}

    // branch counter for debug
    static int brnb = 0;

    //////////////////////////////////////////
    /// parse a branch, this method is recursive so that it parses sub-branches
    static void parseBranch(MethodNode m, AbstractInsnNode inst, Set<AbstractInsnNode> visited, InstructionStack stack) {

        List<Integer> returnopcodes = Arrays.asList(Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN, Opcodes.RETURN);

        if (inst == null || visited.contains(inst)) {
            return;
        }

        visited.add(inst);

        // all the job is done in handleInstruction()
        AbstractInsnNode last = handleInstruction(m, inst, stack);

        // below, we parse other branches if we have a branch instruction
        if (inst instanceof JumpInsnNode) {
            JumpInsnNode jumpInsn = (JumpInsnNode) inst;
            int nb = brnb++;
            parseBranch(m, jumpInsn.label, visited, (InstructionStack)stack.clone());
            if (inst.getOpcode() != Opcodes.GOTO) {
                parseBranch(m, last.getNext(), visited, stack);
            }

        } else 
            if (inst instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode switchInsn = (TableSwitchInsnNode) inst;
                for (LabelNode label : switchInsn.labels) {
                    parseBranch(m, label, visited, (InstructionStack)stack.clone());

                }
                parseBranch(m, switchInsn.dflt, visited, stack);
            } else 
                if (inst instanceof LookupSwitchInsnNode) {
                    LookupSwitchInsnNode lookupInsn = (LookupSwitchInsnNode) inst;
                    for (LabelNode label : lookupInsn.labels) {
                        parseBranch(m, label, visited, (InstructionStack)stack.clone());

                    }
                    parseBranch(m, lookupInsn.dflt, visited, stack);

                } else {
                    if (returnopcodes.contains(inst.getOpcode())) return;
                    parseBranch(m, last.getNext(), visited, stack);
                }
    }


    //////////////////////////////////////////
    /// handle an instruction : that's where we decide how to possibly replace an instruction (or inject new instructions)
    /// rely on the typeTranslation and classTranslation tables above
    /// returns the last handled inst
    static AbstractInsnNode handleInstruction(MethodNode m, AbstractInsnNode inst, InstructionStack stack) {
        
        int op = inst.getOpcode();
        if (op == -1) return inst;

        // ALOAD / ASTORE
        // nothing

        // PUTSTATIC / GETSTATIC / PUTFIELD / GETFIELD
        if (inst instanceof FieldInsnNode) {
            FieldInsnNode fieldinst = (FieldInsnNode)inst;
            String desttype = typeTranslation.get(fieldinst.desc);
            if (desttype != null) {
                fieldinst.desc = desttype;
            }
            stack.handle(inst);
        }
        else 

        // BALOAD / BASTORE 
        if (inst instanceof InsnNode) {
            //InsnNode inst1 = (InsnNode)inst;
            if (op == Opcodes.BALOAD) {
                // we should have on the stack : Pair then index
                // here, we should insert check and access the [B within the Pair object
                StackInst indexinst = stack.pop1();
                StackInst pairinst = stack.pop1();
                stack.push(pairinst);
                AbstractInsnNode last = insertCheck(m.instructions, pairinst.inst, stack);
                AbstractInsnNode next = new FieldInsnNode(Opcodes.GETFIELD, "pack/Pair", "_buff", "[B");
                m.instructions.insert(last, next);
                stack.handle(next);
                stack.push(indexinst);
                stack.handle(inst);
            } else
            if (op == Opcodes.BASTORE) {
                // we should have on the stack : Pair then index then value
                // here, we should insert check and access the [B within the Pair object
                StackInst valueinst = stack.pop1();
                StackInst indexinst = stack.pop1();
                StackInst pairinst = stack.pop1();
                stack.push(pairinst);
                AbstractInsnNode last = insertCheck(m.instructions, pairinst.inst, stack);
                AbstractInsnNode next = new FieldInsnNode(Opcodes.GETFIELD, "pack/Pair", "_buff", "[B");
                m.instructions.insert(last, next);
                stack.handle(next);
                stack.push(indexinst);
                stack.push(valueinst);
                stack.handle(inst);
            } else
            if (op == Opcodes.ARRAYLENGTH) {
                StackInst arrayinst = stack.peek();
                String desc = Util.getADesc(m, stack);
                
                if (desc != null && desc.equals("Lpack/Pair;")) {
                    // we should have on the stack : Pair
                    // here, we should access the [B within the Pair object
                    AbstractInsnNode last = insertCheck(m.instructions, arrayinst.inst, stack);
                    AbstractInsnNode next = new FieldInsnNode(Opcodes.GETFIELD, "pack/Pair", "_buff", "[B");
                    m.instructions.insert(last, next);
                    stack.handle(next);
                    stack.handle(inst);
                } else
                   stack.handle(inst);
            } else
            if (op == Opcodes.ARETURN) {
                stack.handle(inst);
            } else
                stack.handle(inst);
        }
        else

        // NEWARRAY
        if (inst instanceof IntInsnNode) { 
            IntInsnNode inst1 = (IntInsnNode)inst;
            if (op == Opcodes.NEWARRAY) {
                if (inst1.operand == 8) {
                    AbstractInsnNode next;
                    stack.handle(inst);
                    next = new TypeInsnNode(Opcodes.NEW, "pack/Pair");
                    m.instructions.insert(inst, next); 
                    inst = next;
                    stack.handle(inst);
                    // here we have : [B then Pair
                    next = new InsnNode(Opcodes.DUP_X1);
                    m.instructions.insert(inst, next);
                    inst = next;
                    stack.handle(inst);
                    // here we have : Pair then [B then Pair
                    next = new InsnNode(Opcodes.SWAP);
                    m.instructions.insert(inst, next);
                    inst = next;
                    stack.handle(inst);
                    // here we have : Pair then Pair then [B
                    next = new InsnNode(Opcodes.ICONST_1);
                    m.instructions.insert(inst, next);
                    inst = next;
                    stack.handle(inst);
                    // here we have : Pair then Pair then [B then 1
                    next = new MethodInsnNode(Opcodes.INVOKESPECIAL, "pack/Pair", "<init>", "([BZ)V", false);
                    m.instructions.insert(inst, next);
                    inst = next;
                    stack.handle(inst);

                    // here we have : Pair
                } else
                    stack.handle(inst);
            } else 
                stack.handle(inst);
        }
        else

        if (inst instanceof TypeInsnNode) {
            TypeInsnNode inst1 = (TypeInsnNode)inst;
            if (op == Opcodes.NEW) {
                String destclass = classTranslation.get(inst1.desc);
                if (destclass != null)
                    inst1.desc = destclass;
            }
            stack.handle(inst);
            if (op == Opcodes.CHECKCAST && inst1.desc.equals("[B")) {
                AbstractInsnNode next = new MethodInsnNode(Opcodes.INVOKESTATIC, "pack/Pair", "wrap", "([B)Lpack/Pair;", false);
                m.instructions.insert(inst, next);
                inst = next;
                stack.handle(next);
            }
        }
        else
         
        // INVOKEVIRTUAL / INVOKESPECIAL / INVOKESTATIC / INVOKEINTERFACE
        if ((inst instanceof MethodInsnNode) || (inst instanceof InvokeDynamicInsnNode)) {
            boolean outside = true;
            String methname = null, methdesc = null, methowner = null;
            Method meth = null;
            if (inst instanceof MethodInsnNode) {
                MethodInsnNode methodinst = (MethodInsnNode)inst;
                methname = methodinst.name;
                methdesc = methodinst.desc;
                methowner = methodinst.owner;
                meth = new Method(methname, methdesc);
                String newdesc = parseMethodDesc(methname, methdesc, false);

                if (methowner.equals("java/net/http/HttpRequest$Builder") && methname.equals("build")) {
                    InsnList inject = new InsnList();
                    inject.add(new LdcInsnNode("X-ODB"));
                    inject.add(new LdcInsnNode("true"));
                    inject.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/net/http/HttpRequest$Builder", "header", "(Ljava/lang/String;Ljava/lang/String;)Ljava/net/http/HttpRequest$Builder;", true));
                    m.instructions.insertBefore(inst, inject);
                }

                // translate types in method signatures only within the application
                if ((methowner.startsWith("app/"))) {
                    methodinst.desc = newdesc;
                    outside = false;
                } else {
                    String newowner = classTranslation.get(methowner);
                    if (newowner != null) {
                        methodinst.owner = newowner;
                        methodinst.desc = newdesc;
                        if (op == Opcodes.INVOKEINTERFACE) {
                             MethodInsnNode newinst = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, newowner, methodinst.name, newdesc, false);
                             m.instructions.set(inst, newinst);
                             inst = newinst;
                        }
                        outside = false;
                    }
                }
                
            } else {
                InvokeDynamicInsnNode dyninst = (InvokeDynamicInsnNode)inst;
                methname = dyninst.name;
                methdesc = dyninst.desc;

                String newdesc = parseMethodDesc(dyninst.name, dyninst.desc, false);
                dyninst.desc = newdesc;
                meth = new Method(methname, newdesc);

                Object[] bsmArgs = dyninst.bsmArgs;
                for (int i = 0; i < bsmArgs.length; i++) {
                    if (bsmArgs[i] instanceof Handle) {
                        Handle h = (Handle) bsmArgs[i];
                        String newOwner = classTranslation.get(h.getOwner());
                        if (newOwner == null && h.getOwner().startsWith("app/")) newOwner = h.getOwner();

                        String hNewDesc = parseMethodDesc(h.getName(), h.getDesc(), false);
                        if (newOwner != null || !hNewDesc.equals(h.getDesc())) {
                             bsmArgs[i] = new Handle(h.getTag(), newOwner != null ? newOwner : h.getOwner(), h.getName(), hNewDesc, h.isInterface());
                        }
                    } else if (bsmArgs[i] instanceof Type) {
                        Type t = (Type) bsmArgs[i];
                        if (t.getSort() == Type.METHOD) {
                            String tNewDesc = parseMethodDesc("", t.getDescriptor(), false);
                            if (!tNewDesc.equals(t.getDescriptor())) {
                                bsmArgs[i] = Type.getMethodType(tNewDesc);
                            }
                        } else if (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY) {
                            String dest = typeTranslation.get(t.getDescriptor());
                            if (dest != null) {
                                bsmArgs[i] = Type.getType(dest);
                            }
                        }
                    }
                }
            }
if (outside) {
    // Unwrap Pair → [B pour paramètres
    Type[] args = meth.getArgumentTypes();
    Stack<StackInst> st = new Stack<StackInst>();
    for (int i = args.length - 1; i >= 0; i--) {
        StackInst v = stack.pop();
        if (args[i].getDescriptor().equals("[B")) {
            AbstractInsnNode last = insertCheck(m.instructions, v.inst, stack);
            AbstractInsnNode next = new FieldInsnNode(Opcodes.GETFIELD, "pack/Pair", "_buff", "[B");
            m.instructions.insert(last, next);
            st.push(new OneSlotInst(next));
        } else {
            st.push(v);
        }
    }
    for (int i = 0; i < args.length; i++) stack.push(st.pop());
    
    stack.handle(inst);
    
    // Wrapper les retours
    String retdesc = meth.getReturnType().getDescriptor();
    String desttype = typeTranslation.get(retdesc);
    if (desttype != null) {
        AbstractInsnNode next;
        
        if (retdesc.equals("[B")) {
            next = new MethodInsnNode(Opcodes.INVOKESTATIC, "pack/Pair", "wrap", "([B)Lpack/Pair;", false);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(inst);
        } 
        else if (retdesc.equals("Ljava/io/InputStream;")) {
            next = new TypeInsnNode(Opcodes.NEW, "odb/MyInputStream");
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new InsnNode(Opcodes.DUP_X1);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new InsnNode(Opcodes.SWAP);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new InsnNode(Opcodes.ICONST_0);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new InsnNode(Opcodes.ACONST_NULL);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new InsnNode(Opcodes.ICONST_0);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);

            next = new MethodInsnNode(Opcodes.INVOKESPECIAL, "odb/MyInputStream", "<init>", "(Ljava/io/InputStream;ZLjava/io/ObjectInputStream;Z)V", false);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
        }
        else if (retdesc.equals("Ljakarta/servlet/ServletInputStream;")) {
            next = new TypeInsnNode(Opcodes.NEW, "odb/MyServletInputStream");
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new InsnNode(Opcodes.DUP_X1);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new InsnNode(Opcodes.SWAP);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new MethodInsnNode(Opcodes.INVOKESPECIAL, "odb/MyServletInputStream", "<init>", "(Ljakarta/servlet/ServletInputStream;)V", false);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
        }
        else if (retdesc.equals("Ljakarta/servlet/ServletOutputStream;")) {
            next = new TypeInsnNode(Opcodes.NEW, "odb/MyServletOutputStream");
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new InsnNode(Opcodes.DUP_X1);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new InsnNode(Opcodes.SWAP);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
            
            next = new MethodInsnNode(Opcodes.INVOKESPECIAL, "odb/MyServletOutputStream", "<init>", "(Ljakarta/servlet/ServletOutputStream;)V", false);
            m.instructions.insert(inst, next);
            inst = next;
            stack.handle(next);
        }
    }
} else {
    stack.handle(inst);
}
        } 
        else
            stack.handle(inst);
        return inst;
    }

     //////////////////////////////////////////
     /// insert the check after the instruction pairinst (the instruction which pushed the Pair)
     /// returns the last inserted instruction 
     
     static AbstractInsnNode insertCheck(InsnList instructions, AbstractInsnNode pairinst, InstructionStack stack) {

        // call handler
        AbstractInsnNode next, inst = pairinst;
        next = new InsnNode(Opcodes.DUP);
        instructions.insert(inst, next);
        inst = next;
        stack.handle(inst);
        next = new FieldInsnNode(Opcodes.GETFIELD, "pack/Pair", "_access", "Z");
        instructions.insert(inst, next);
        inst = next;
        stack.handle(inst);
        LabelNode labelContinue = new LabelNode();
        next = new JumpInsnNode(Opcodes.IFNE, labelContinue);
        instructions.insert(inst, next);
        inst = next;
        stack.handle(inst);
        next = new InsnNode(Opcodes.DUP);
        instructions.insert(inst, next);
        inst = next;
        stack.handle(inst);
        next = new MethodInsnNode(Opcodes.INVOKESTATIC,"pack/Handler","bufferFault","(Lpack/Pair;)V",false);
        instructions.insert(inst, next);
        inst = next;
        stack.handle(inst);
        next = labelContinue;
        instructions.insert(inst, next);
        inst = next;
        stack.handle(inst);

        return inst;
     }


    //////////////////////////////////////////
    

    public static void parseExceptionHandlers(MethodNode m) {
        
        List<TryCatchBlockNode> ltc = m.tryCatchBlocks;
        Set<AbstractInsnNode> visited = new HashSet<AbstractInsnNode>();
        for (TryCatchBlockNode tcb : ltc) {
            LabelNode label = tcb.handler;
            InstructionStack stack = new InstructionStack();
            stack.push(new OneSlotInst(null)); // exception that was pushed on the stack
            AbstractInsnNode inst = label;
            parseBranch(m, inst, visited, stack);
        }

    }
    
    public static void main(String args[]) {
        
        try {
            ClassNode cn = new ClassNode(Opcodes.ASM4);
            ClassReader cr = new ClassReader(args[0]);
            cr.accept(cn, 0);

            cn.signature = null;
            List<FieldNode> lf = parseFields(cn.fields);
            cn.fields = lf;

            for (MethodNode m : cn.methods) {
                m.signature = null;
                m.desc = parseMethodDesc(m.name,m.desc, true);
                parseLocalVariables(m);
                parseInstructions(m);
                parseExceptionHandlers(m);
            }


            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            byte[] b = cw.toByteArray();

            File outputFile = new File("out/"+cn.name+".class");
            if (outputFile.exists()) outputFile.delete();
            outputFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(b);
            fos.close();
            System.out.println("Parsed and saved: " + args[0]);

        } catch (Exception ex) {
            System.err.println("Exception during parsing of " + args[0]);
            ex.printStackTrace();
        }
    }
    
}
