package com.pclewis.mcpatcher;

import javassist.bytecode.Bytecode;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Mnemonic;

import static javassist.bytecode.Opcode.*;

class ConstPoolUtils {
    private static final byte[] DUMMY_BYTES = "zzzz nothing zzzz".getBytes();

    public static int getTag(Object o) {
        if (o instanceof Float) {
            return ConstPool.CONST_Float;
        } else if (o instanceof Double) {
            return ConstPool.CONST_Double;
        } else if (o instanceof Integer) {
            return ConstPool.CONST_Integer;
        } else if (o instanceof Long) {
            return ConstPool.CONST_Long;
        } else if (o instanceof String) {
            return ConstPool.CONST_String;
        } else if (o instanceof MethodRef) {
            return ConstPool.CONST_Methodref;
        } else if (o instanceof InterfaceMethodRef) {
            return ConstPool.CONST_InterfaceMethodref;
        } else if (o instanceof FieldRef) {
            return ConstPool.CONST_Fieldref;
        } else if (o instanceof ClassRef) {
            return ConstPool.CONST_Class;
        }
        throw new AssertionError("Unreachable");
    }

    private static int addToPool(ConstPool cp, Object o) {
        if (o instanceof Float) {
            return cp.addFloatInfo((Float) o);
        } else if (o instanceof Double) {
            return cp.addDoubleInfo((Double) o);
        } else if (o instanceof Integer) {
            return cp.addIntegerInfo((Integer) o);
        } else if (o instanceof Long) {
            return cp.addLongInfo((Long) o);
        } else if (o instanceof String) {
            return cp.addStringInfo((String) o);
        } else if (o instanceof MethodRef) {
            MethodRef mr = (MethodRef) o;
            int ci = findOrAdd(cp, new ClassRef(mr.getClassName().replaceAll("\\.", "/")));
            return cp.addMethodrefInfo(ci, mr.getName(), mr.getType());
        } else if (o instanceof InterfaceMethodRef) {
            InterfaceMethodRef imr = (InterfaceMethodRef) o;
            int ci = findOrAdd(cp, new ClassRef(imr.getClassName().replaceAll("\\.", "/")));
            return cp.addInterfaceMethodrefInfo(ci, imr.getName(), imr.getType());
        } else if (o instanceof FieldRef) {
            FieldRef fr = (FieldRef) o;
            int ci = findOrAdd(cp, new ClassRef(fr.getClassName().replaceAll("\\.", "/")));
            return cp.addFieldrefInfo(ci, fr.getName(), fr.getType());
        } else if (o instanceof ClassRef) {
            return cp.addClassInfo(((ClassRef) o).getClassName());
        }
        throw new AssertionError("Unreachable");
    }

    public static boolean checkEqual(ConstPool cp, int index, Object o) {
        if (o instanceof Float) {
            return cp.getFloatInfo(index) == (Float) o;
        } else if (o instanceof Double) {
            return cp.getDoubleInfo(index) == (Double) o;
        } else if (o instanceof Integer) {
            return cp.getIntegerInfo(index) == (Integer) o;
        } else if (o instanceof Long) {
            return cp.getLongInfo(index) == (Long) o;
        } else if (o instanceof String) {
            return o.equals(cp.getStringInfo(index));
        } else if (o instanceof JavaRef) {
            return ((JavaRef) o).checkEqual(cp, index);
        }
        throw new AssertionError("Unreachable");
    }

    private static int find(ConstPool cp, Object value) {
        int index = -1;
        int tag = getTag(value);
        for (int i = 1; i < cp.getSize(); ++i) {
            if (cp.getTag(i) == tag) {
                if (checkEqual(cp, i, value)) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    private static int findOrAdd(ConstPool cp, Object value) {
        int index = ConstPoolUtils.find(cp, value);
        if (index == -1) {
            index = ConstPoolUtils.addToPool(cp, value);
        }
        return index;
    }

    private static byte[] getLoad(ConstPool cp, Object value) {
        int index = findOrAdd(cp, value);
        int op = Bytecode.LDC;
        if (value instanceof Double) {
            op = Bytecode.LDC2_W;
        }
        return getLoad(op, index);
    }

    private static byte[] getLoad(int op, int i) {
        int mop = op;
        if (i >= Byte.MAX_VALUE && mop == Bytecode.LDC)
            mop = Bytecode.LDC_W;
        if (mop == Bytecode.LDC) {
            return new byte[]{(byte) mop, Util.b(i, 0)};
        } else {
            return new byte[]{(byte) mop, Util.b(i, 1), Util.b(i, 0)};
        }
    }

    public static byte[] push(ConstPool cp, Object value, boolean add) {
        if (value instanceof Integer) {
            int i = (Integer) value;
            switch (i) {
                case 0:
                    return new byte[]{ICONST_0};
                case 1:
                    return new byte[]{ICONST_1};
                case 2:
                    return new byte[]{ICONST_2};
                case 3:
                    return new byte[]{ICONST_3};
                case 4:
                    return new byte[]{ICONST_4};
                case 5:
                    return new byte[]{ICONST_5};
                default:
                    break;
            }
            if (i <= Byte.MAX_VALUE) {
                return new byte[]{BIPUSH, (byte) i};
            } else if (i <= Short.MAX_VALUE) {
                return new byte[]{SIPUSH, Util.b(i, 1), Util.b(i, 0)};
            }
        } else if (value instanceof Float) {
            float f = (Float) value;
            if (f == 0.0f) {
                return new byte[]{FCONST_0};
            } else if (f == 1.0f) {
                return new byte[]{FCONST_1};
            } else if (f == 2.0f) {
                return new byte[]{FCONST_2};
            }
        } else if (value instanceof Double) {
            double d = (Double) value;
            if (d == 0.0) {
                return new byte[]{DCONST_0};
            } else if (d == 1.0) {
                return new byte[]{DCONST_1};
            }
        }
        int index = add ? findOrAdd(cp, value) : find(cp, value);
        if (index >= 0) {
            return getLoad((value instanceof Double || value instanceof Long ? LDC2_W : LDC), index);
        } else {
            return DUMMY_BYTES;
        }
    }

    public static byte[] reference(ConstPool cp, Object value, boolean add) {
        int index = add ? findOrAdd(cp, value) : find(cp, value);
        if (index < 0) {
            return DUMMY_BYTES;
        }
        return Util.marshal16(index);
    }

    public static byte[] reference(ConstPool cp, int opcode, Object value, boolean add) {
        int index = add ? findOrAdd(cp, value) : find(cp, value);
        if (index < 0) {
            return DUMMY_BYTES;
        }
        switch (opcode) {
            case LDC:
            case LDC2_W:
                return getLoad(opcode, index);

            case GETFIELD:
            case GETSTATIC:
                if (!(value instanceof FieldRef)) {
                    throw new AssertionError(Mnemonic.OPCODE[opcode] + " requires a FieldRef object");
                }
                break;

            case INVOKEVIRTUAL:
            case INVOKESTATIC:
            case INVOKESPECIAL:
                if (!(value instanceof MethodRef)) {
                    throw new AssertionError(Mnemonic.OPCODE[opcode] + " requires a MethodRef object");
                }
                break;

            case INVOKEINTERFACE:
                if (!(value instanceof InterfaceMethodRef)) {
                    throw new AssertionError(Mnemonic.OPCODE[opcode] + " requires an InterfaceMethodRef object");
                }
                break;

            case INSTANCEOF:
            case CHECKCAST:
            case MULTIANEWARRAY:
            case ANEWARRAY:
            case NEW:
                if (!(value instanceof ClassRef)) {
                    throw new AssertionError(Mnemonic.OPCODE[opcode] + " requires a ClassRef object");
                }
                break;

            default:
                break;
        }
        return new byte[]{(byte) opcode, Util.b(index, 1), Util.b(index, 0)};
    }
}
