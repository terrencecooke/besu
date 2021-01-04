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
package org.hyperledger.besu.ethereum.p2p.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.ethereum.p2p.discovery.internal.ENRRequestPacketData;
import org.hyperledger.besu.ethereum.p2p.discovery.internal.ENRResponsePacketData;
import org.hyperledger.besu.ethereum.p2p.discovery.internal.FindNeighborsPacketData;
import org.hyperledger.besu.ethereum.p2p.discovery.internal.NeighborsPacketData;
import org.hyperledger.besu.ethereum.p2p.discovery.internal.Packet;
import org.hyperledger.besu.ethereum.p2p.discovery.internal.PacketType;
import org.hyperledger.besu.ethereum.p2p.discovery.internal.PingPacketData;
import org.hyperledger.besu.ethereum.p2p.discovery.internal.PongPacketData;
import org.hyperledger.besu.util.NetworkUtility;

import java.time.Instant;

import com.google.common.net.InetAddresses;
import io.vertx.core.buffer.Buffer;
import org.apache.tuweni.units.bigints.UInt64;
import org.assertj.core.api.Condition;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class PeerDiscoveryPacketPcapSedesTest {
  private static final String pingHexData =
      "dcb59003d39fe8cf9bdc72fb83cd5f83493d8e564f9bca0ef496479b928786f7fd56981f3b"
          + "1b921edc5b28417f964179a731d690ea6a0595292dad3a45f8d1ae535efe5545290a8855086722f3f22ad821146172e865c8bdc0a78d14"
          + "4d76fef80001e505cb847f00000182765f82765fc9847f00000282765f80845fda127a880000000000000003";
  private static final String pongHexData =
      "53cec0d27af44bdc0471d34c4eb631f74b502df7b5513a80a054f0d619f0417d6ba4fd4d6f"
          + "b83994b95c6d0ae8b175b068a6bffc397e2b408e797069b9370ce47b153dd884b60108e686546a775ed5f85e71059a9c5791e266bd949d"
          + "0dcfba380102f83bcb84b4b57a1a82040182765fa046896547d3b4259aa1a67bd26e7ec58ab4be650c5552ef0360caf9dae489d53b845b"
          + "872dc8880000000000000003";
  private static final String findNeighborsHexData =
      "3b4c3be981427a8e9739dcd4ea3cf29fe1faa104b8381cb7c26053c4b711015b3"
          + "919213819e30284bb82ec90081098ff4af02e8d9aa12692d4a0511fe92a3c137c3b65dddc309a0384ddb60074be46735c798710f04b95a"
          + "868a1fdbac9328bc70003f847b840986165a2febf6b2b69383bfe10bfeafe1e0d63eac2387d340e51f402bf98860323dd8603800b661ce"
          + "df5823e1a478f4f78e6661c957ed1db1b146d521cf60675845fda14be";
  private static final String neighborsHexData =
      "fa4484fd625113e9bf1d38218d98ce8c28f31d722f38b0bb1bc8296c82c741e8490ac"
          + "82ea9afcb582f393cd5b7ad7fc72990015d3cc58f7f1527b6a60f671767458bc4cd4c00a08ab0eb56c85b5ab739bfda68b7cf24cdbb99d"
          + "3dddbd4e0c6840004f8a5f89ef84d847f00000182765f82765fb840233590850744c0d95e3fd325a2b694de5d3a0f1e0c7e304358253f5"
          + "725d25734a2e08bb1c2ac4ccccd527660b8f1a265c0dae4ef6adda8b5f07a742239bbd1fff84d847f00000182765f82765fb840841d92a"
          + "de4223b36e213e03197fecc1250f34c52e1e1ec8cdff5b9cbe005f95567daa9fd96a64c0e3e3a8d55157bf9d87f1c4666cdae79b37bfa5"
          + "c1835353475845fda165c";
  private static final String enrResquestHexData =
      "08218a2075159e8e48d5bc0561b3c9b107c4459454b82b2a0df818222178a7128e4"
          + "3e951a7ee1669b5486458919bb6c5f9e910a400222eeabe12191d234b5ce94b5614cbf19c23ed4de001ffcdd009cbb38fa0ac475e272ae"
          + "593ecc5e4cff2e90105c605845fda17cc";
  private static final String enrResponseHexData =
      "f354dd86f7651dad9a4ab3845c985a0a603a049b8b9dd563d26ad4ecbd6d9d066c0"
          + "f448523df89a396d2ee1bf3758d34d750ae4e46956824a11b9fcb39a757551f70be6f7a6692638c16cf903e241e6c777125ce377dd7bd0"
          + "7139d4845a2d8f50106f8e7a008218a2075159e8e48d5bc0561b3c9b107c4459454b82b2a0df818222178a712b8c4f8c2b860000000000"
          + "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
          + "000000000000000000000000000000000000000000000000000000000000000000000000003826964827634826970893132372e302e302"
          + "e3189736563703235366b31b840fbe12329d5d99e3d46cba2d1f9d8d397a4f2955253396f6e0459f3f14bb29c0e4f37d8bac890ff9bfb4"
          + "12879257ba2378a0b48bed6b81647c6972d323212d051";

  @Test
  public void testUDPPingSerializeDeserialize() {
    final byte[] data = Hex.decode(pingHexData);
    final Packet packet = Packet.decode(Buffer.buffer(data));
    assertThat(packet.getType()).isNotNull();
    assertThat(packet.getNodeId()).isNotNull();
    assertThat(packet.getNodeId().toArray()).hasSize(64);

    assertThat(packet.getType()).isEqualTo(PacketType.PING);
    assertThat(packet.getPacketData(PingPacketData.class)).isPresent();
    final PingPacketData pingPacketData = packet.getPacketData(PingPacketData.class).orElse(null);

    assertThat(pingPacketData).isNotNull();
    assertThat(pingPacketData.getTo()).isNotNull();
    assertThat(pingPacketData.getFrom()).isNotNull();
    assertThat(pingPacketData.getTo().getHost()).satisfies(validInetAddressCondition);
    assertThat(pingPacketData.getFrom().map(Endpoint::getHost))
        .hasValueSatisfying(validInetAddressCondition);
    assertThat(pingPacketData.getTo().getUdpPort()).isPositive();
    assertThat(pingPacketData.getFrom().get().getUdpPort()).isPositive();
    pingPacketData.getTo().getTcpPort().ifPresent(p -> assertThat(p).isPositive());
    pingPacketData.getFrom().get().getTcpPort().ifPresent(p -> assertThat(p).isPositive());
    assertThat(pingPacketData.getExpiration()).isPositive();
    assertThat(pingPacketData.getEnrSeq().isPresent()).isTrue();
    assertThat(pingPacketData.getEnrSeq().get()).isGreaterThan(UInt64.ZERO);

    final byte[] encoded = packet.encode().getBytes();
    assertThat(encoded).isEqualTo(data);
  }

  @Test
  public void testUDPPongSerializeDeserialize() {
    final byte[] data = Hex.decode(pongHexData);
    final Packet packet = Packet.decode(Buffer.buffer(data));
    assertThat(packet.getType()).isNotNull();
    assertThat(packet.getNodeId()).isNotNull();
    assertThat(packet.getNodeId().toArray()).hasSize(64);
    assertThat(packet.getType()).isEqualTo(PacketType.PONG);
    assertThat(packet.getPacketData(PongPacketData.class)).isPresent();

    final PongPacketData pongPacketData = packet.getPacketData(PongPacketData.class).orElse(null);
    assertThat(pongPacketData).isNotNull();
    assertThat(pongPacketData.getTo()).isNotNull();
    assertThat(pongPacketData.getTo().getHost()).satisfies(validInetAddressCondition);
    assertThat(pongPacketData.getTo().getUdpPort()).isPositive();
    pongPacketData.getTo().getTcpPort().ifPresent(p -> assertThat(p).isPositive());
    assertThat(pongPacketData.getPingHash().toArray()).hasSize(32);
    assertThat(pongPacketData.getExpiration()).isPositive();
    assertThat(pongPacketData.getEnrSeq().isPresent()).isTrue();
    assertThat(pongPacketData.getEnrSeq().get()).isGreaterThan(UInt64.ZERO);

    final byte[] encoded = packet.encode().getBytes();
    assertThat(encoded).isEqualTo(data);
  }

  @Test
  public void testUDPFindNeighborsSerializeDeserialize() {
    final byte[] data = Hex.decode(findNeighborsHexData);
    final Packet packet = Packet.decode(Buffer.buffer(data));
    final Instant timestamp = Instant.ofEpochSecond(1608127678L);
    assertThat(packet.getType()).isNotNull();
    assertThat(packet.getNodeId()).isNotNull();
    assertThat(packet.getNodeId().toArray()).hasSize(64);
    assertThat(packet.getType()).isEqualTo(PacketType.FIND_NEIGHBORS);
    assertThat(packet.getPacketData(FindNeighborsPacketData.class)).isPresent();

    final FindNeighborsPacketData findNeighborsPacketData =
        packet.getPacketData(FindNeighborsPacketData.class).orElse(null);
    assertThat(findNeighborsPacketData).isNotNull();
    assertThat(findNeighborsPacketData.getExpiration())
        .isBetween(timestamp.getEpochSecond() - 10000, timestamp.getEpochSecond() + 10000);
    assertThat(findNeighborsPacketData.getTarget().toArray()).hasSize(64);
    assertThat(packet.getNodeId().toArray()).hasSize(64);

    final byte[] encoded = packet.encode().getBytes();
    assertThat(encoded).isEqualTo(data);
  }

  @Test
  public void testUDPNeighborsSerializeDeserialize() {
    final byte[] data = Hex.decode(neighborsHexData);
    final Packet packet = Packet.decode(Buffer.buffer(data));
    assertThat(packet.getType()).isNotNull();
    assertThat(packet.getNodeId()).isNotNull();
    assertThat(packet.getNodeId().toArray()).hasSize(64);
    assertThat(packet.getType()).isEqualTo(PacketType.NEIGHBORS);
    assertThat(packet.getPacketData(NeighborsPacketData.class)).isPresent();

    final NeighborsPacketData neighborsPacketData =
        packet.getPacketData(NeighborsPacketData.class).orElse(null);
    assertThat(neighborsPacketData).isNotNull();
    assertThat(neighborsPacketData.getExpiration()).isPositive();
    assertThat(neighborsPacketData.getNodes()).isNotEmpty();

    for (final DiscoveryPeer p : neighborsPacketData.getNodes()) {
      assertThat(NetworkUtility.isValidPort(p.getEndpoint().getUdpPort())).isTrue();
      assertThat(p.getEndpoint().getHost()).satisfies(validInetAddressCondition);
      assertThat(p.getId().toArray()).hasSize(64);
    }

    final byte[] encoded = packet.encode().getBytes();
    assertThat(encoded).isEqualTo(data);
  }

  @Test
  public void testUDPENRRequestSerializeDeserialize() {
    final byte[] data = Hex.decode(enrResquestHexData);
    final Packet packet = Packet.decode(Buffer.buffer(data));
    assertThat(packet.getType()).isNotNull();
    assertThat(packet.getNodeId()).isNotNull();
    assertThat(packet.getNodeId().toArray()).hasSize(64);
    assertThat(packet.getType()).isEqualTo(PacketType.ENR_REQUEST);

    final ENRRequestPacketData enrRequestPacketData =
        packet.getPacketData(ENRRequestPacketData.class).orElse(null);
    assertThat(enrRequestPacketData).isNotNull();
    assertThat(enrRequestPacketData.getExpiration()).isPositive();

    final byte[] encoded = packet.encode().getBytes();
    assertThat(encoded).isEqualTo(data);
  }

  @Test
  public void testUDPENRResponseSerializeDeserialize() {
    final byte[] data = Hex.decode(enrResponseHexData);
    final Packet packet = Packet.decode(Buffer.buffer(data));
    assertThat(packet.getType()).isNotNull();
    assertThat(packet.getNodeId()).isNotNull();
    assertThat(packet.getNodeId().toArray()).hasSize(64);
    assertThat(packet.getType()).isEqualTo(PacketType.ENR_RESPONSE);

    final ENRResponsePacketData enrResponsePacketData =
        packet.getPacketData(ENRResponsePacketData.class).orElse(null);
    assertThat(enrResponsePacketData).isNotNull();
    assertThat(enrResponsePacketData.getEnr()).isNotNull();
    assertThat(enrResponsePacketData.getEnr().getSeq()).isGreaterThan(UInt64.ZERO);
    assertThat(enrResponsePacketData.getEnr().getSignature()).isNotNull();
    assertThat(enrResponsePacketData.getRequestHash()).isNotNull();
    assertThat(enrResponsePacketData.getRequestHash().toArray()).hasSize(32);

    final byte[] encoded = packet.encode().getBytes();
    assertThat(encoded).isEqualTo(data);
  }

  private final Condition<String> validInetAddressCondition =
      new Condition<>(InetAddresses::isInetAddress, "checks for valid InetAddresses");
}
