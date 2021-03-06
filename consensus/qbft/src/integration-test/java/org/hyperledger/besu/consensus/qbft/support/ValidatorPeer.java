/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.support;

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.EventMultiplexer;
import org.hyperledger.besu.consensus.common.bft.inttest.DefaultValidatorPeer;
import org.hyperledger.besu.consensus.common.bft.inttest.NodeParams;
import org.hyperledger.besu.consensus.qbft.messagedata.CommitMessageData;
import org.hyperledger.besu.consensus.qbft.messagedata.PrepareMessageData;
import org.hyperledger.besu.consensus.qbft.messagedata.ProposalMessageData;
import org.hyperledger.besu.consensus.qbft.messagedata.RoundChangeMessageData;
import org.hyperledger.besu.consensus.qbft.messagewrappers.Commit;
import org.hyperledger.besu.consensus.qbft.messagewrappers.Prepare;
import org.hyperledger.besu.consensus.qbft.messagewrappers.Proposal;
import org.hyperledger.besu.consensus.qbft.messagewrappers.RoundChange;
import org.hyperledger.besu.consensus.qbft.payload.MessageFactory;
import org.hyperledger.besu.consensus.qbft.payload.RoundChangeMetadata;
import org.hyperledger.besu.consensus.qbft.statemachine.PreparedRoundArtifacts;
import org.hyperledger.besu.crypto.SECP256K1.Signature;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.Hash;

import java.util.Collections;
import java.util.Optional;

// Each "inject" function returns the SignedPayload representation of the transmitted message.
public class ValidatorPeer extends DefaultValidatorPeer {

  private final MessageFactory messageFactory;

  public ValidatorPeer(
      final NodeParams nodeParams,
      final MessageFactory messageFactory,
      final EventMultiplexer localEventMultiplexer) {
    super(nodeParams, localEventMultiplexer);
    this.messageFactory = messageFactory;
  }

  public Proposal injectProposal(final ConsensusRoundIdentifier rId, final Block block) {
    final Proposal payload =
        messageFactory.createProposal(rId, block, Collections.emptyList(), Collections.emptyList());

    injectMessage(ProposalMessageData.create(payload));
    return payload;
  }

  public Prepare injectPrepare(final ConsensusRoundIdentifier rId, final Hash digest) {
    final Prepare payload = messageFactory.createPrepare(rId, digest);
    injectMessage(PrepareMessageData.create(payload));
    return payload;
  }

  public Commit injectCommit(final ConsensusRoundIdentifier rId, final Hash digest) {
    final Signature commitSeal = nodeKey.sign(digest);

    return injectCommit(rId, digest, commitSeal);
  }

  public Commit injectCommit(
      final ConsensusRoundIdentifier rId, final Hash digest, final Signature commitSeal) {
    final Commit payload = messageFactory.createCommit(rId, digest, commitSeal);
    injectMessage(CommitMessageData.create(payload));
    return payload;
  }

  public Proposal injectProposalForFutureRound(
      final ConsensusRoundIdentifier rId,
      final RoundChangeMetadata roundChangeMetadata,
      final Block blockToPropose) {

    final Proposal payload =
        messageFactory.createProposal(
            rId,
            blockToPropose,
            roundChangeMetadata.getRoundChangePayloads(),
            roundChangeMetadata.getPrepares());
    injectMessage(ProposalMessageData.create(payload));
    return payload;
  }

  public RoundChange injectRoundChange(
      final ConsensusRoundIdentifier rId,
      final Optional<PreparedRoundArtifacts> preparedRoundArtifacts) {
    final RoundChange payload =
        messageFactory.createRoundChange(
            rId, preparedRoundArtifacts.map(PreparedRoundArtifacts::getPreparedCertificate));
    injectMessage(RoundChangeMessageData.create(payload));
    return payload;
  }

  public MessageFactory getMessageFactory() {
    return messageFactory;
  }
}
