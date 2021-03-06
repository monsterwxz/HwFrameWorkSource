package android.icu.impl.number;

import android.icu.text.DateFormat;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class DecimalQuantity_DualStorageBCD extends DecimalQuantity_AbstractBCD {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private byte[] bcdBytes;
    private long bcdLong = 0;
    private boolean usingBytes = false;

    public int maxRepresentableDigits() {
        return Integer.MAX_VALUE;
    }

    public DecimalQuantity_DualStorageBCD() {
        setBcdToZero();
        this.flags = (byte) 0;
    }

    public DecimalQuantity_DualStorageBCD(long input) {
        setToLong(input);
    }

    public DecimalQuantity_DualStorageBCD(int input) {
        setToInt(input);
    }

    public DecimalQuantity_DualStorageBCD(double input) {
        setToDouble(input);
    }

    public DecimalQuantity_DualStorageBCD(BigInteger input) {
        setToBigInteger(input);
    }

    public DecimalQuantity_DualStorageBCD(BigDecimal input) {
        setToBigDecimal(input);
    }

    public DecimalQuantity_DualStorageBCD(DecimalQuantity_DualStorageBCD other) {
        copyFrom(other);
    }

    public DecimalQuantity_DualStorageBCD(Number number) {
        if (number instanceof Long) {
            setToLong(number.longValue());
        } else if (number instanceof Integer) {
            setToInt(number.intValue());
        } else if (number instanceof Double) {
            setToDouble(number.doubleValue());
        } else if (number instanceof BigInteger) {
            setToBigInteger((BigInteger) number);
        } else if (number instanceof BigDecimal) {
            setToBigDecimal((BigDecimal) number);
        } else if (number instanceof android.icu.math.BigDecimal) {
            setToBigDecimal(((android.icu.math.BigDecimal) number).toBigDecimal());
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Number is of an unsupported type: ");
            stringBuilder.append(number.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public DecimalQuantity createCopy() {
        return new DecimalQuantity_DualStorageBCD(this);
    }

    protected byte getDigitPos(int position) {
        if (this.usingBytes) {
            if (position < 0 || position > this.precision) {
                return (byte) 0;
            }
            return this.bcdBytes[position];
        } else if (position < 0 || position >= 16) {
            return (byte) 0;
        } else {
            return (byte) ((int) ((this.bcdLong >>> (position * 4)) & 15));
        }
    }

    protected void setDigitPos(int position, byte value) {
        if (this.usingBytes) {
            ensureCapacity(position + 1);
            this.bcdBytes[position] = value;
        } else if (position >= 16) {
            switchStorage();
            ensureCapacity(position + 1);
            this.bcdBytes[position] = value;
        } else {
            int shift = position * 4;
            this.bcdLong = (this.bcdLong & (~(15 << shift))) | (((long) value) << shift);
        }
    }

    protected void shiftLeft(int numDigits) {
        if (!this.usingBytes && this.precision + numDigits > 16) {
            switchStorage();
        }
        if (this.usingBytes) {
            ensureCapacity(this.precision + numDigits);
            int i = (this.precision + numDigits) - 1;
            while (i >= numDigits) {
                this.bcdBytes[i] = this.bcdBytes[i - numDigits];
                i--;
            }
            while (i >= 0) {
                this.bcdBytes[i] = (byte) 0;
                i--;
            }
        } else {
            this.bcdLong <<= numDigits * 4;
        }
        this.scale -= numDigits;
        this.precision += numDigits;
    }

    protected void shiftRight(int numDigits) {
        if (this.usingBytes) {
            int i = 0;
            while (i < this.precision - numDigits) {
                this.bcdBytes[i] = this.bcdBytes[i + numDigits];
                i++;
            }
            while (i < this.precision) {
                this.bcdBytes[i] = (byte) 0;
                i++;
            }
        } else {
            this.bcdLong >>>= numDigits * 4;
        }
        this.scale += numDigits;
        this.precision -= numDigits;
    }

    protected void setBcdToZero() {
        if (this.usingBytes) {
            this.bcdBytes = null;
            this.usingBytes = false;
        }
        this.bcdLong = 0;
        this.scale = 0;
        this.precision = 0;
        this.isApproximate = false;
        this.origDouble = 0.0d;
        this.origDelta = 0;
    }

    protected void readIntToBcd(int n) {
        long result = 0;
        int n2 = n;
        n = 16;
        while (n2 != 0) {
            result = (result >>> 4) + ((((long) n2) % 10) << 60);
            n2 /= 10;
            n--;
        }
        this.bcdLong = result >>> (n * 4);
        this.scale = 0;
        this.precision = 16 - n;
    }

    protected void readLongToBcd(long n) {
        if (n >= 10000000000000000L) {
            ensureCapacity();
            long n2 = n;
            int i = 0;
            while (n2 != 0) {
                this.bcdBytes[i] = (byte) ((int) (n2 % 10));
                n2 /= 10;
                i++;
            }
            this.scale = 0;
            this.precision = i;
            return;
        }
        int i2 = 16;
        long result = 0;
        long n3 = n;
        while (n3 != 0) {
            result = (result >>> 4) + ((n3 % 10) << 60);
            n3 /= 10;
            i2--;
        }
        this.bcdLong = result >>> (i2 * 4);
        this.scale = 0;
        this.precision = 16 - i2;
    }

    protected void readBigIntegerToBcd(BigInteger n) {
        ensureCapacity();
        BigInteger n2 = n;
        int i = 0;
        while (n2.signum() != 0) {
            BigInteger[] temp = n2.divideAndRemainder(BigInteger.TEN);
            ensureCapacity(i + 1);
            this.bcdBytes[i] = temp[1].byteValue();
            n2 = temp[0];
            i++;
        }
        this.scale = 0;
        this.precision = i;
    }

    protected BigDecimal bcdToBigDecimal() {
        if (this.usingBytes) {
            BigDecimal result = new BigDecimal(toNumberString());
            if (isNegative()) {
                result = result.negate();
            }
            return result;
        }
        long tempLong = 0;
        for (int shift = this.precision - 1; shift >= 0; shift--) {
            tempLong = (10 * tempLong) + ((long) getDigitPos(shift));
        }
        BigDecimal result2 = BigDecimal.valueOf(tempLong).scaleByPowerOfTen(this.scale);
        if (isNegative()) {
            result2 = result2.negate();
        }
        return result2;
    }

    protected void compact() {
        int delta;
        if (this.usingBytes) {
            delta = 0;
            while (delta < this.precision && this.bcdBytes[delta] == (byte) 0) {
                delta++;
            }
            if (delta == this.precision) {
                setBcdToZero();
                return;
            }
            shiftRight(delta);
            int leading = this.precision - 1;
            while (leading >= 0 && this.bcdBytes[leading] == (byte) 0) {
                leading--;
            }
            this.precision = leading + 1;
            if (this.precision <= 16) {
                switchStorage();
            }
        } else if (this.bcdLong == 0) {
            setBcdToZero();
        } else {
            delta = Long.numberOfTrailingZeros(this.bcdLong) / 4;
            this.bcdLong >>>= delta * 4;
            this.scale += delta;
            this.precision = 16 - (Long.numberOfLeadingZeros(this.bcdLong) / 4);
        }
    }

    private void ensureCapacity() {
        ensureCapacity(40);
    }

    private void ensureCapacity(int capacity) {
        if (capacity != 0) {
            int oldCapacity = this.usingBytes ? this.bcdBytes.length : 0;
            if (!this.usingBytes) {
                this.bcdBytes = new byte[capacity];
            } else if (oldCapacity < capacity) {
                byte[] bcd1 = new byte[(capacity * 2)];
                System.arraycopy(this.bcdBytes, 0, bcd1, 0, oldCapacity);
                this.bcdBytes = bcd1;
            }
            this.usingBytes = true;
        }
    }

    private void switchStorage() {
        int i = 0;
        int i2;
        if (this.usingBytes) {
            this.bcdLong = 0;
            for (i2 = this.precision - 1; i2 >= 0; i2--) {
                this.bcdLong <<= 4;
                this.bcdLong |= (long) this.bcdBytes[i2];
            }
            this.bcdBytes = null;
            this.usingBytes = false;
            return;
        }
        ensureCapacity();
        while (true) {
            i2 = i;
            if (i2 < this.precision) {
                this.bcdBytes[i2] = (byte) ((int) (this.bcdLong & 15));
                this.bcdLong >>>= 4;
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    protected void copyBcdFrom(DecimalQuantity _other) {
        DecimalQuantity_DualStorageBCD other = (DecimalQuantity_DualStorageBCD) _other;
        setBcdToZero();
        if (other.usingBytes) {
            ensureCapacity(other.precision);
            System.arraycopy(other.bcdBytes, 0, this.bcdBytes, 0, other.precision);
            return;
        }
        this.bcdLong = other.bcdLong;
    }

    @Deprecated
    public String checkHealth() {
        int i = 0;
        int i2;
        if (!this.usingBytes) {
            if (this.bcdBytes != null) {
                for (byte b : this.bcdBytes) {
                    if (b != (byte) 0) {
                        return "Nonzero digits in byte array but we are in long mode";
                    }
                }
            }
            if (this.precision == 0 && this.bcdLong != 0) {
                return "Value in bcdLong even though precision is zero";
            }
            if (this.precision > 16) {
                return "Precision exceeds length of long";
            }
            if (this.precision != 0 && getDigitPos(this.precision - 1) == (byte) 0) {
                return "Most significant digit is zero in long mode";
            }
            if (this.precision != 0 && getDigitPos(0) == (byte) 0) {
                return "Least significant digit is zero in long mode";
            }
            while (true) {
                i2 = i;
                if (i2 >= this.precision) {
                    for (i2 = this.precision; i2 < 16; i2++) {
                        if (getDigitPos(i2) != (byte) 0) {
                            return "Nonzero digits outside of range in long";
                        }
                    }
                } else if (getDigitPos(i2) >= (byte) 10) {
                    return "Digit exceeding 10 in long";
                } else {
                    if (getDigitPos(i2) < (byte) 0) {
                        return "Digit below 0 in long (?!)";
                    }
                    i = i2 + 1;
                }
            }
        } else if (this.bcdLong != 0) {
            return "Value in bcdLong but we are in byte mode";
        } else {
            if (this.precision == 0) {
                return "Zero precision but we are in byte mode";
            }
            if (this.precision > this.bcdBytes.length) {
                return "Precision exceeds length of byte array";
            }
            if (getDigitPos(this.precision - 1) == (byte) 0) {
                return "Most significant digit is zero in byte mode";
            }
            if (getDigitPos(0) == (byte) 0) {
                return "Least significant digit is zero in long mode";
            }
            while (true) {
                i2 = i;
                if (i2 >= this.precision) {
                    for (i2 = this.precision; i2 < this.bcdBytes.length; i2++) {
                        if (getDigitPos(i2) != (byte) 0) {
                            return "Nonzero digits outside of range in byte array";
                        }
                    }
                } else if (getDigitPos(i2) >= (byte) 10) {
                    return "Digit exceeding 10 in byte array";
                } else {
                    if (getDigitPos(i2) < (byte) 0) {
                        return "Digit below 0 in byte array";
                    }
                    i = i2 + 1;
                }
            }
        }
        return null;
    }

    @Deprecated
    public boolean isUsingBytes() {
        return this.usingBytes;
    }

    public String toString() {
        String str = "<DecimalQuantity %s:%d:%d:%s %s %s>";
        Object[] objArr = new Object[6];
        objArr[0] = this.lOptPos > 1000 ? "999" : String.valueOf(this.lOptPos);
        objArr[1] = Integer.valueOf(this.lReqPos);
        objArr[2] = Integer.valueOf(this.rReqPos);
        objArr[3] = this.rOptPos < -1000 ? "-999" : String.valueOf(this.rOptPos);
        objArr[4] = this.usingBytes ? "bytes" : "long";
        objArr[5] = toNumberString();
        return String.format(str, objArr);
    }

    public String toNumberString() {
        StringBuilder sb = new StringBuilder();
        if (this.usingBytes) {
            for (int i = this.precision - 1; i >= 0; i--) {
                sb.append(this.bcdBytes[i]);
            }
        } else {
            sb.append(Long.toHexString(this.bcdLong));
        }
        sb.append(DateFormat.ABBR_WEEKDAY);
        sb.append(this.scale);
        return sb.toString();
    }
}
