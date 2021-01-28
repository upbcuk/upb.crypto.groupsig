package de.upb.crypto.groupsig.common;

import de.upb.crypto.math.serialization.Representable;

/**
 * Proves that a specific signatures was created by a specific group member.
 * Verified using {@link GroupSignatureScheme#openVerify(Integer, OpenProof, GroupSignature)}.
 *
 * <p>It can be restored from its representation via the appropriate method from {@link GroupSignatureScheme}.
 */
public interface OpenProof extends Representable {
}
