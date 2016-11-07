package io.syncpoint.dtn;

import org.junit.Assert;
import org.junit.Test;

public class HelperTest {

    static final String DCI = "<DCI xsi:noNamespaceSchemaLocation=\"DEMConnectInfoDoc.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <DciScope>ANNOUNCE</DciScope>\n" +
            "  <DciBody>\n" +
            "    <NodeID>123000123</NodeID>\n" +
            "    <ReplicationNodeIPAddress>192.168.168.68</ReplicationNodeIPAddress>\n" +
            "    <ReplicationNodePort>10000</ReplicationNodePort>\n" +
            "    <ResponsibleOrgName>THX-ORG</ResponsibleOrgName>\n" +
            "    <RoleName>DTN</RoleName>\n" +
            "    <MipReleaseVersion>3.1</MipReleaseVersion>\n" +
            "  </DciBody>\n" +
            "</DCI>";

    static final String T_OPEN_REQUEST = "00000022T1|123456789|987654321";
    static final String SUPERVISOR_ADDRESS = "dtn://some.host/dem/111000111";
    static final String CHANNEL_ADDRESS = "dtn://some.host/dem/111000111/888999888";


    @Test
    public void findElementValue$NodeID() {
        final String nodeID = Helper.getElementValue("NodeID", DCI);
        Assert.assertEquals("123000123", nodeID);
    }

    @Test
    public void findElementValue$ReplicationNodeIPAddress() {
        final String ReplicationNodeIPAddress = Helper.getElementValue("ReplicationNodeIPAddress", DCI);
        Assert.assertEquals("192.168.168.68", ReplicationNodeIPAddress);
    }

    @Test
    public void findElementValue$ReplicationNodePort() {
        final String ReplicationNodePort = Helper.getElementValue("ReplicationNodePort", DCI);
        Assert.assertEquals("10000", ReplicationNodePort);
    }

    @Test
    public void nodeIdFromTOpenRequest$source () {
        final String sourceNodeId = Helper.getSourceNodeId(T_OPEN_REQUEST);
        Assert.assertEquals("123456789", sourceNodeId);
    }

    @Test
    public void nodeIdFromTOpenRequest$destination () {
        final String destinationNodeId = Helper.getDestinationNodeId(T_OPEN_REQUEST);
        Assert.assertEquals("987654321", destinationNodeId);
    }

    @Test
    public void address$host() {
        Assert.assertEquals("dtn://some.host", Helper.getDtnHostFromUri(SUPERVISOR_ADDRESS));
    }

    @Test
    public void address$path() {
        Assert.assertEquals("111000111", Helper.getChannelFromUri(SUPERVISOR_ADDRESS, Addresses.APP_PREFIX));
    }

    @Test
    public void address$channel() {
        Assert.assertEquals("111000111/888999888", Helper.getChannelFromUri(CHANNEL_ADDRESS, Addresses.APP_PREFIX));
    }


}
