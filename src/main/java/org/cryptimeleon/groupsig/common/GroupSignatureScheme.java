package org.cryptimeleon.groupsig.common;

import org.cryptimeleon.craco.common.plaintexts.PlainText;
import org.cryptimeleon.craco.sig.Signature;
import org.cryptimeleon.math.serialization.Representation;
import org.cryptimeleon.math.serialization.StandaloneRepresentable;
import org.cryptimeleon.math.serialization.annotations.RepresentationRestorer;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;

/**
 * Interfaces for Group Signatures designed according to [DAR15].
 *
 * <p>This interface does not contain a setup method since parameterization can differ a lot there.
 * Setup such as establishing the bilinear group (in case of a pairing-based scheme) is usually done in a separate
 * class.
 *
 * <p>[DAR15] Jesus Diaz and David Arroyo and Francisco B. Rodriguez,
 * libgroupsig: An extensible C library for group signatures, Cryptology ePrint Archive, Report 2015/1146,
 * https://eprint.iacr.org/2015/1146.
 */
public interface GroupSignatureScheme extends StandaloneRepresentable, RepresentationRestorer {

    /**
     * Member part of the join protocol.
     *
     * <p>If the join protocol is an interactive one, this method should be run in parallel with
     * {@link GroupSignatureScheme#joinIssuer(IssuerKey, GroupMembershipList, BlockingQueue, BlockingQueue)}.
     *
     * @param received Contains the messages sent by the issuer to the member
     * @param sent Contains the messages sent by the member to the issuer
     *
     * @throws InterruptedException if interrupted while waiting for one of the queues
     *
     * @return The issued {@link MemberKey}
     */
    MemberKey joinMember(BlockingQueue<Representation> received, BlockingQueue<Representation> sent) throws InterruptedException;

    /**
     * Issuer part of the join protocol.
     *
     * <p>If the join protocol is an interactive one, this method should be run in parallel with
     * {@link GroupSignatureScheme#joinMember(BlockingQueue, BlockingQueue)}.
     *
     * @param issuerKey The {@link IssuerKey} used to issue the key for the new group member.
     * @param gml The {@link GroupMembershipList} containing information about each member.
     *            Gets updated with new member information if join succeeds.
     * @param received Contains the messages sent by the member to the issuer
     * @param sent Contains the messages sent by the issuer to the member
     *
     * @throws InterruptedException if interrupted while waiting for one of the queues
     */
    void joinIssuer(IssuerKey issuerKey, GroupMembershipList gml, BlockingQueue<Representation> received,
                    BlockingQueue<Representation> sent) throws InterruptedException;

    /**
     * Sign the given plain text with the given member key.
     *
     * @param plainText The {@link PlainText} to sign
     * @param memberKey The {@link MemberKey} of the group member that wants to sign
     * @return Signature over the given plain text
     */
    GroupSignature sign(PlainText plainText, MemberKey memberKey);

    /**
     * Verifies that the given signature was correctly created for the given message.
     *
     * <p>Group signature schemes with verifier-local revocation allow the verifier to check whether the signature
     * creator was revoked via the given revocation list.
     *
     * @param plainText The {@link PlainText} for the signature to verify
     * @param signature The {@link GroupSignature} to verify
     * @param revocationList The {@link RevocationList} that allows for verifier-local revocation if offered by the
     *                       scheme
     * @return {@code true} if verification succeeds, else {@code false}
     */
    Boolean verify(PlainText plainText, GroupSignature signature, RevocationList revocationList);

    /**
     * Verifies that the given signature was correctly created for the given message.
     *
     * @param plainText The {@link PlainText} for the signature to verify
     * @param signature The {@link GroupSignature} to verify
     * @return {@code true} if verification succeeds, else {@code false}
     */
    default Boolean verify(PlainText plainText, GroupSignature signature) {
        return verify(plainText, signature, null);
    }

    /**
     * Issues a {@link ClaimProof} proving that the given signature was created for the given group and member keys.
     *
     * @param memberKey The {@link MemberKey} of the member to prove signing for
     * @param signature The {@link GroupSignature} to prove the claim for
     * @return A {@link ClaimProof} proving correspondence of the given signature to the member and group keys
     */
    ClaimProof claim(MemberKey memberKey, GroupSignature signature) throws UnsupportedOperationException;

    /**
     * Verifies that the given {@link ClaimProof} is correct for the given signature and group.
     *
     * @param proof The {@link ClaimProof} to verify
     * @param signature The {@link GroupSignature} to verify the claim for
     * @return {@code true} if the verification succeeds, else {@code false}
     */
    Boolean claimVerify(ClaimProof proof, GroupSignature signature) throws UnsupportedOperationException;

    /**
     * Reveals the identity of the group member that issued the given signature and, optionally,
     * creates a proof of opening.
     *
     * @param signature The {@link GroupSignature} whose issuer should be revealed
     * @param openerKey The private {@link OpenerKey} of the group manager or opener
     * @param gml The {@link GroupMembershipList} containing information about the members
     * @param revocationList The {@link RevocationList} containing revocation information
     * @return An {@link OpenResult} containing the member identity and an {@link OpenProof} that can be used
     *         to verify correctness of the open process
     */
    OpenResult open(GroupSignature signature, OpenerKey openerKey, GroupMembershipList gml,
                    RevocationList revocationList);

    /**
     * Reveals the identity of the group member that issued the given signature and, optionally,
     * creates a proof of opening.
     * Same as {@link GroupSignatureScheme#open(GroupSignature, OpenerKey, GroupMembershipList, RevocationList)}
     * but without requiring a revocation list argument.
     *
     * @param signature The {@link GroupSignature} whose issuer should be revealed
     * @param openerKey The private {@link OpenerKey} of the group manager or opener
     * @param gml The {@link GroupMembershipList} containing information about the members
     * @return An {@link OpenResult} containing the member identity and an {@link OpenProof} that can be used
     *         to verify correctness of the open process
     */
    default OpenResult open(GroupSignature signature, OpenerKey openerKey, GroupMembershipList gml) {
        return open(signature, openerKey, gml, null);
    }

    /**
     * Verifies the proof of opening for the given signature and group.
     *
     * @param memberIdentity The member identity that was revealed
     * @param openProof The proof of opening
     * @param signature The {@link GroupSignature} whose issuer was revealed
     * @return {@code true} if the verification succeeds, else {@code false}
     * @throws UnsupportedOperationException if the scheme does not support this operation
     */
    Boolean openVerify(Integer memberIdentity, OpenProof openProof, GroupSignature signature)
            throws UnsupportedOperationException;

    /**
     * Verifies the proof of opening for the given signature and group.
     *
     * @param openResult The {@link OpenResult} produced by calling
     *                   {@link GroupSignatureScheme#open(GroupSignature, OpenerKey, GroupMembershipList, RevocationList)}
     * @param signature The {@link GroupSignature} whose issuer was revealed
     * @return {@code true} if the verification succeeds, else {@code false}
     * @throws UnsupportedOperationException if the scheme does not support this operation
     */
    default Boolean openVerify(OpenResult openResult, GroupSignature signature) {
        return openVerify(openResult.getMemberIdentity(), openResult.getOpenProof(), signature);
    }

    /**
     * Adds the tracing trapdoor of the group member specified by the given member identity in the given
     * {@link GroupMembershipList} to the given {@link RevocationList}.
     *
     * @param gml The {@link GroupMembershipList} containing group information
     * @param memberIdentity The member identity of the group member to reveal
     * @param revocationList The {@link RevocationList} to store the revealed tracing trapdoor in
     * @throws UnsupportedOperationException if the scheme does not support this operation
     */
    void reveal(GroupMembershipList gml, Integer memberIdentity, RevocationList revocationList)
            throws UnsupportedOperationException;

    /**
     * Determines whether or not the issuer of the specified signature has been revoked according to the given
     * {@link RevocationList}.
     *
     * @param signature The {@link GroupSignature} to trace
     * @param revocationList The {@link RevocationList} containing revealed trapdoors used for tracing
     * @param openerKey The {@link OpenerKey} used for tracing
     * @param gml The {@link GroupMembershipList} containing group member information
     * @return {@code true} if the verification succeeds, else {@code false}
     * @throws UnsupportedOperationException if the scheme does not support this operation
     */
    Boolean trace(GroupSignature signature, RevocationList revocationList, OpenerKey openerKey,
                  GroupMembershipList gml) throws UnsupportedOperationException;

    MemberKey restoreMemberKey(Representation repr);

    OpenerKey restoreOpenerKey(Representation repr);

    IssuerKey restoreIssuerKey(Representation repr);

    GroupSignature restoreSignature(Representation repr);

    PlainText restorePlainText(Representation repr);

    GMLEntry restoreGmlEntry(Representation repr);

    GroupMembershipList restoreGroupMembershipList(Representation repr);

    RevocationListEntry restoreRevocationListEntry(Representation repr);

    RevocationList restoreRevocationList(Representation repr);

    /**
     * Provides an injective mapping of the byte[] to a {@link PlainText} usable with this scheme (which may be a
     * MessageBlock).
     * It only guarantees injectivity for arrays of the same length. Applications that would like to use mapToPlaintext
     * with multiple different array lengths, may want to devise a padding method and then only call mapToPlaintext with
     * byte[] of the same (padded) length.
     * This method may throw an {@link IllegalArgumentException} if there is no injective {@link PlainText} element of
     * these bytes (e.g., the byte array is too long).
     *
     * @param bytes Bytes to be mapped to a {@link PlainText}
     * @return The corresponding plaintext
     */
    PlainText mapToPlaintext(byte[] bytes);

    /**
     * As described in {@link #mapToPlaintext} there might be no injective {@link PlainText} for some byte arrays, e.g.
     * if the byte array is too long. Therefore, this method provides the maximal number of bytes that can be mapped
     * injectively to a {@link PlainText}.
     *
     * @return maximal number of bytes that can be given to {@link #mapToPlaintext}.
     */
    int getMaxNumberOfBytesForMapToPlaintext();

    default Object restoreFromRepresentation(Type type, Representation repr) {
        if (type instanceof Class) {
            if (MemberKey.class.isAssignableFrom((Class) type)) {
                return this.restoreMemberKey(repr);
            } else if (OpenerKey.class.isAssignableFrom((Class) type)) {
                return this.restoreOpenerKey(repr);
            } else if (IssuerKey.class.isAssignableFrom((Class) type)) {
                return this.restoreIssuerKey(repr);
            } else if (Signature.class.isAssignableFrom((Class) type)) {
                return this.restoreSignature(repr);
            } else if (PlainText.class.isAssignableFrom((Class) type)) {
                return this.restorePlainText(repr);
            } else if (GMLEntry.class.isAssignableFrom((Class) type)) {
                return this.restoreGmlEntry(repr);
            } else if (GroupMembershipList.class.isAssignableFrom((Class) type)) {
                return this.restoreGroupMembershipList(repr);
            } else if (RevocationListEntry.class.isAssignableFrom((Class) type)) {
                return this.restoreRevocationListEntry(repr);
            } else if (RevocationList.class.isAssignableFrom((Class) type)) {
                return this.restoreRevocationList(repr);
            }
        }
        throw new IllegalArgumentException("Cannot recreate object of type: " + type.getTypeName());
    }
}
