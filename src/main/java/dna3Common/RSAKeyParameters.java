package dna3Common;

import java.math.BigInteger;

public class RSAKeyParameters {
    private BigInteger m_modulus;

    private BigInteger m_exponent;

    public RSAKeyParameters(boolean isPrivate, BigInteger modulus, BigInteger exponent) {
        this.m_modulus = modulus;
        this.m_exponent = exponent;
    }

    public BigInteger getModulus() {
        return this.m_modulus;
    }

    public BigInteger getExponent() {
        return this.m_exponent;
    }
}
