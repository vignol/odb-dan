package pack;

import java.util.Stack;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class InstructionStack extends Stack<StackInst> {

    public StackInst pop1() {
        if (isEmpty()) throw new RuntimeException("Stack empty when pop1 expected");
        if (peek() instanceof OneSlotInst) return pop();
        else {
            throw new RuntimeException("inconsistent stack state: expected OneSlotInst but got " + peek().getClass().getSimpleName());
        }
    }
    public StackInst pop2() {
        if (isEmpty()) throw new RuntimeException("Stack empty when pop2 expected");
        if (peek() instanceof TwoSlotInst) return pop();
        else {
            throw new RuntimeException("inconsistent stack state: expected TwoSlotInst but got " + peek().getClass().getSimpleName());
        }
    }

    public void handle(AbstractInsnNode inst) {

        if (inst.getOpcode() == -1) return;

        StackInst v1,v2,v3,v4;

        switch (inst.getOpcode()) {
            // --- Constants ---
            case Opcodes.CHECKCAST:
                pop1(); // Pop the object to be cast
                push(new OneSlotInst(inst));
                break;
            case Opcodes.NOP:
                break; 
                
            case Opcodes.ACONST_NULL:
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                push(new OneSlotInst(inst));
                break; 
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                push(new TwoSlotInst(inst));
                break; 
            case Opcodes.LDC:
                LdcInsnNode ldcinst = (LdcInsnNode)inst;
                Object val = ldcinst.cst;
                if ((val instanceof Long) || (val instanceof Double)) {
                    push(new TwoSlotInst(inst));
                } else
                    push(new OneSlotInst(inst));
                break;
            
            // --- local variables  ---
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
            case Opcodes.ALOAD:
                push(new OneSlotInst(inst));
                break; 
            case Opcodes.LLOAD:
            case Opcodes.DLOAD:
                push(new TwoSlotInst(inst));
                break; 
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
            case Opcodes.ASTORE:
                pop1();
                break; 
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
                pop2();
                break; 
            case Opcodes.IINC:
                break;

            // --- Arithmetic operations ---
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IXOR:
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
                pop1();pop1();
                push(new OneSlotInst(inst));
                break; 
            case Opcodes.INEG:
            case Opcodes.FNEG:
                pop1();
                push(new OneSlotInst(inst));
                break;
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.LREM:
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
                pop2();pop2();
                push(new TwoSlotInst(inst));
                break; 
            case Opcodes.LNEG:
            case Opcodes.DNEG:
                pop2();
                push(new TwoSlotInst(inst));
                break;
            case Opcodes.DCMPG:
            case Opcodes.DCMPL:
                pop2();pop2();
                push(new OneSlotInst(inst));
                break;
            case Opcodes.FCMPG:
            case Opcodes.FCMPL:
                pop1();pop1();
                push(new OneSlotInst(inst));
                break;
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
                pop1();pop2();
                push(new TwoSlotInst(inst));
                break;
                

            // --- Conditional branch ---
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                pop1();
                break;
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
                pop1();pop1();
                break;

            // --- Flow control ---
            case Opcodes.GOTO:
            case Opcodes.JSR:
                break; 
            case Opcodes.RET:
                break; 

            // --- Array instructions ---
            case Opcodes.ARRAYLENGTH:
            case Opcodes.ANEWARRAY:
            case Opcodes.NEWARRAY:
                pop1();
                push(new OneSlotInst(inst));
                break;
            case Opcodes.MULTIANEWARRAY:
                MultiANewArrayInsnNode arrayinst = (MultiANewArrayInsnNode)inst;
                String desc = arrayinst.desc;
                int nbdim = 0;
                while (desc.charAt(nbdim) == '[') {
                    nbdim++;
                    pop1();
                }
                push(new OneSlotInst(inst));
                break;

            case Opcodes.IALOAD:
            case Opcodes.FALOAD:
            case Opcodes.AALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
                pop1();pop1();
                push(new OneSlotInst(inst));
                break;
            case Opcodes.LALOAD:
            case Opcodes.DALOAD:
                pop1();pop1();
                push(new TwoSlotInst(inst));
                break;
            case Opcodes.IASTORE:
            case Opcodes.FASTORE:
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
                pop1();pop1();pop1();
                break;
            case Opcodes.LASTORE:
            case Opcodes.DASTORE:
                pop1();pop1();pop2();
                break;

            // --- Conversion instructions  ---
            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.I2D:
            case Opcodes.F2D:
                pop1();
                push(new TwoSlotInst(inst));
                break;
            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.D2I:
            case Opcodes.D2F:
                pop2();
                push(new OneSlotInst(inst));
                break;
            case Opcodes.I2F:
            case Opcodes.F2I:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                pop1();
                push(new OneSlotInst(inst));
                break;
            case Opcodes.D2L:
            case Opcodes.L2D:
                pop2();
                push(new TwoSlotInst(inst));
                break;
            case Opcodes.LAND:
                pop2();pop2();
                push(new TwoSlotInst(inst));
                break;
            case Opcodes.LCMP:
                pop2();pop2();
                push(new OneSlotInst(inst));
                break;

            // --- Return instructions ---
            case Opcodes.RETURN:
                break;
            case Opcodes.IRETURN:
            case Opcodes.FRETURN:
            case Opcodes.ARETURN:
                pop1();
                break;
            case Opcodes.LRETURN:
            case Opcodes.DRETURN:
                pop2();
                break;

            // --- Field instructions ---
            case Opcodes.GETSTATIC: {
                FieldInsnNode fieldinst = (FieldInsnNode) inst;
                if ((fieldinst.desc.equals("J")) || (fieldinst.desc.equals("D"))) {
                    push(new TwoSlotInst(inst));
                } else
                    push(new OneSlotInst(inst));
                }
                break;
            case Opcodes.PUTSTATIC: {
                FieldInsnNode fieldinst = (FieldInsnNode) inst;
                if ((fieldinst.desc.equals("J")) || (fieldinst.desc.equals("D"))) {
                    pop2();
                } else
                    pop1();
                }
                break;
            case Opcodes.GETFIELD: {
                FieldInsnNode fieldinst = (FieldInsnNode) inst;
                pop1();
                if ((fieldinst.desc.equals("J")) || (fieldinst.desc.equals("D"))) {
                    push(new TwoSlotInst(inst));
                } else
                    push(new OneSlotInst(inst));
                }
                break;
            case Opcodes.PUTFIELD: {
                FieldInsnNode fieldinst = (FieldInsnNode) inst;
                pop1();
                if ((fieldinst.desc.equals("J")) || (fieldinst.desc.equals("D"))) {
                    pop2();
                } else
                    pop1();
                }
                break;

            // --- Object instructions ---
            case Opcodes.NEW:
                push(new OneSlotInst(inst));
                break;

            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEINTERFACE:
                pop1();
            case Opcodes.INVOKEDYNAMIC:
            case Opcodes.INVOKESTATIC:
                Method meth = null;
                if (inst instanceof MethodInsnNode) {
                    MethodInsnNode methodinst = (MethodInsnNode) inst;
                    meth = new Method(methodinst.name, methodinst.desc);
                }
                if (inst instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode methodinst = (InvokeDynamicInsnNode) inst;
                    meth = new Method(methodinst.name, methodinst.desc);
                }
                Type[] args = meth.getArgumentTypes();
                for (int i = args.length - 1; i >= 0; i--) {
                    String d = args[i].getDescriptor();
                    if (d.equals("J") || d.equals("D")) {
                        pop2();
                    } else {
                        pop1();
                    }
                }
                String retdesc = meth.getReturnType().getDescriptor();
                if (retdesc.equals("V")) {
                    break;
                } else
                if (retdesc.equals("J") || retdesc.equals("D")) {
                    push(new TwoSlotInst(inst));
                } else {
                    push(new OneSlotInst(inst));
                }
                break;
            case Opcodes.INSTANCEOF:
                pop1();
                push(new OneSlotInst(inst));
                break;

            // --- Special instructions ---
            case Opcodes.POP:
                pop1();
                break; 
            case Opcodes.POP2:
                if (peek() instanceof OneSlotInst) {
                    pop1();pop1();
                } else pop2();
                break;
            case Opcodes.DUP:
                if (!(peek() instanceof OneSlotInst)) {
                    throw new RuntimeException("inconsistent DUP: expected OneSlotInst");
                }
                push(peek());
                break; 
            case Opcodes.DUP2:
                if (peek() instanceof OneSlotInst) {
                    v1=pop1();v2=pop1();
                    push(v2);push(v1);push(v2);push(v1);
                } else {
                    push(peek());
                }
                break; 
            case Opcodes.DUP_X1:
                v1=pop1();v2=pop1();
                if ((v1 instanceof TwoSlotInst) || (v2 instanceof TwoSlotInst)) {
                    throw new RuntimeException("inconsistent DUP_X1: unexpected TwoSlotInst");
                }
                push(v1);push(v2);push(v1);
                break;
            case Opcodes.DUP_X2:
                v1=pop1();
                if (peek() instanceof OneSlotInst) {
                    v2=pop1();v3=pop1();
                    push(v1);push(v3);push(v2);push(v1);
                } else {
                    v2=pop2();
                    push(v1);push(v2);push(v1);
                }
                break; 
            case Opcodes.DUP2_X1:
                v1=pop1();v2=pop1();v3=pop1();
                push(v2);push(v1);push(v3);push(v2);push(v1);
                break; 
            case Opcodes.DUP2_X2:
                v1=pop1();v2=pop1();v3=pop();v4=pop();
                push(v2);push(v1);push(v4);push(v3);push(v2);push(v1);
                break; 
            case Opcodes.SWAP:
                v1=pop1();v2=pop1();
                push(v1);push(v2);
                break;

            // --- Synchronisation ---
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                pop1();
                break;

            // --- Exceptions ---
            case Opcodes.ATHROW:
                pop1();
                break;

            // --- Switch instructions ---
            case Opcodes.TABLESWITCH:
            case Opcodes.LOOKUPSWITCH:
                pop1();
                break;

            case Opcodes.F_NEW:
                break;
                
            default:
                throw new RuntimeException("Opcode non supporté : " + Util.getOpcodeName(inst.getOpcode()) + " (" + inst.getOpcode() + ")");
        }
    }

}
