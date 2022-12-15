/*
 * (c) Copyright Luca Bressan. Contact: contact.lucabressan@icloud.com
 *
 * Created December 15th, 2022.
 */
package net.finmath.randomnumbers;

import java.security.SecureRandom;

/**
 * Wrapper class for java.security.SecureRandom. Usage of this class is not
 * recommended for normal operations.
 * There are reported cases in the literature where some pseudorandom number
 * generators might fail in heavily multithreaded applications and lose their
 * statistical properties. This class wraps a cryptography grade RNG based on
 * the system entropy pool (eg. /dev/random on *nix systems) which does not
 * suffer from the same issue. Performance is up to 30x slower than a regular
 * software based PRNG.
 *
 * @author Luca Bressan
 *
 * @version 1.0
 */
public class HighEntropyRandomNumberGenerator implements RandomNumberGenerator1D {

    private static final long serialVersionUID = -818028598001664L;
    private SecureRandom secureRandomNumberGenerator;

    public HighEntropyRandomNumberGenerator() {
        super();
        this.secureRandomNumberGenerator = new SecureRandom();
    }

    /**
     * This constructor accepts an object from classes that extend
     * java.security.SecureRandom. It is meant to provide a way to integrate
     * hardware RNG functionality or as a clone constructor. You should supply an
     * object instantiating the extending class that uses the JNI to
     * achieve the desired functionality.
     * 
     * @param secureRandomNumberGenerator the object that will be wrapped.
     */
    public HighEntropyRandomNumberGenerator(SecureRandom secureRandomNumberGenerator) {
        super();
        this.secureRandomNumberGenerator = secureRandomNumberGenerator;
    }

    /**
	 * Returns the next random number in the sequence.
	 *
	 * @return the next random number in the sequence.
	 */
    @Override
    public double nextDouble() {
        synchronized (secureRandomNumberGenerator) {
            return secureRandomNumberGenerator.nextDouble();
        }

    }

    @Override
    public double nextDoubleFast() {
        return secureRandomNumberGenerator.nextDouble();
    }

    @Override
    public String toString() {
        return "HighEntropyRandomNumberGenrator [algorithm = " + secureRandomNumberGenerator.getAlgorithm() + "]";
    }
}
