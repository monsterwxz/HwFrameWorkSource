package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.AbstractF2m;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECLookupTable;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.raw.Nat192;
import org.bouncycastle.util.encoders.Hex;

public class SecT163R1Curve extends AbstractF2m {
    private static final int SecT163R1_DEFAULT_COORDS = 6;
    protected SecT163R1Point infinity;

    public SecT163R1Curve() {
        super(CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384, 3, 6, 7);
        this.infinity = new SecT163R1Point(this, null, null);
        this.a = fromBigInteger(new BigInteger(1, Hex.decode("07B6882CAAEFA84F9554FF8428BD88E246D2782AE2")));
        this.b = fromBigInteger(new BigInteger(1, Hex.decode("0713612DCDDCB40AAB946BDA29CA91F73AF958AFD9")));
        this.order = new BigInteger(1, Hex.decode("03FFFFFFFFFFFFFFFFFFFF48AAB689C29CA710279B"));
        this.cofactor = BigInteger.valueOf(2);
        this.coord = 6;
    }

    protected ECCurve cloneCurve() {
        return new SecT163R1Curve();
    }

    public ECLookupTable createCacheSafeLookupTable(ECPoint[] eCPointArr, int i, final int i2) {
        final long[] jArr = new long[((i2 * 3) * 2)];
        int i3 = 0;
        int i4 = i3;
        while (i3 < i2) {
            ECPoint eCPoint = eCPointArr[i + i3];
            Nat192.copy64(((SecT163FieldElement) eCPoint.getRawXCoord()).x, 0, jArr, i4);
            i4 += 3;
            Nat192.copy64(((SecT163FieldElement) eCPoint.getRawYCoord()).x, 0, jArr, i4);
            i4 += 3;
            i3++;
        }
        return new ECLookupTable() {
            public int getSize() {
                return i2;
            }

            public ECPoint lookup(int i) {
                long[] create64 = Nat192.create64();
                long[] create642 = Nat192.create64();
                int i2 = 0;
                int i3 = i2;
                while (i2 < i2) {
                    long j = (long) (((i2 ^ i) - 1) >> 31);
                    for (int i4 = 0; i4 < 3; i4++) {
                        create64[i4] = create64[i4] ^ (jArr[i3 + i4] & j);
                        create642[i4] = create642[i4] ^ (jArr[(i3 + 3) + i4] & j);
                    }
                    i3 += 6;
                    i2++;
                }
                return SecT163R1Curve.this.createRawPoint(new SecT163FieldElement(create64), new SecT163FieldElement(create642), false);
            }
        };
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        return new SecT163R1Point(this, eCFieldElement, eCFieldElement2, z);
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        return new SecT163R1Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr, z);
    }

    public ECFieldElement fromBigInteger(BigInteger bigInteger) {
        return new SecT163FieldElement(bigInteger);
    }

    public int getFieldSize() {
        return CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384;
    }

    public ECPoint getInfinity() {
        return this.infinity;
    }

    public int getK1() {
        return 3;
    }

    public int getK2() {
        return 6;
    }

    public int getK3() {
        return 7;
    }

    public int getM() {
        return CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384;
    }

    public boolean isKoblitz() {
        return false;
    }

    public boolean isTrinomial() {
        return false;
    }

    public boolean supportsCoordinateSystem(int i) {
        return i == 6;
    }
}
