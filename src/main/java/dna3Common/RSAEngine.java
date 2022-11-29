package dna3Common;

import java.math.BigInteger;

public class RSAEngine {
    private RSAKeyParameters mKey;

    private boolean mForEncryption;

    public void init(boolean forEncryption, RSAKeyParameters rsaKeyParameters) {
        this.mKey = rsaKeyParameters;
        assert this.mKey != null;
        assert this.mKey.getModulus() != null;
        assert this.mKey.getExponent() != null;
        this.mForEncryption = forEncryption;
    }

    public int getInputBlockSize() {
        int bitSize = this.mKey.getModulus().bitLength();
        if (this.mForEncryption)
            return (bitSize + 7) / 8 - 1;
        return (bitSize + 7) / 8;
    }

    public int getOutputBlockSize() {
        int bitSize = this.mKey.getModulus().bitLength();
        if (this.mForEncryption)
            return (bitSize + 7) / 8;
        return (bitSize + 7) / 8 - 1;
    }

    public byte[] processBlock(byte[] in, int inOff, int inLen) throws DataLengthException {
        byte[] block;
        if (inLen > getInputBlockSize() + 1)
            throw new DataLengthException("RSAEngine.processBlock: input too large for RSA cipher : " + inLen + " > " + (getInputBlockSize() + 1) + ".\n");
        if (inLen == getInputBlockSize() + 1 && (in[inOff] & 0x80) != 0)
            throw new DataLengthException("RSAEngine.processBlock: input too large for RSA cipher.\n");
        if (inOff != 0 || inLen != in.length) {
            block = new byte[inLen];
            System.arraycopy(in, inOff, block, 0, inLen);
        } else {
            block = in;
        }
        BigInteger input = new BigInteger(1, block);
        byte[] output = input.modPow(this.mKey.getExponent(), this.mKey.getModulus()).toByteArray();
        if (this.mForEncryption) {
            if (output[0] == 0 && output.length > getOutputBlockSize()) {
                byte[] tmp = new byte[output.length - 1];
                System.arraycopy(output, 1, tmp, 0, tmp.length);
                return tmp;
            }
            if (output.length < getOutputBlockSize()) {
                byte[] tmp = new byte[getOutputBlockSize()];
                System.arraycopy(output, 0, tmp, tmp.length - output.length, output.length);
                return tmp;
            }
        } else if (output[0] == 0) {
            byte[] tmp = new byte[output.length - 1];
            System.arraycopy(output, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return output;
    }
}
