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

    @Test
    public void findElementValue$NodeID() {
        final String nodeID = Helper.findElementValue("NodeID", DCI);
        Assert.assertEquals("123000123", nodeID);
    }

    @Test
    public void findElementValue$ReplicationNodeIPAddress() {
        final String ReplicationNodeIPAddress = Helper.findElementValue("ReplicationNodeIPAddress", DCI);
        Assert.assertEquals("192.168.168.68", ReplicationNodeIPAddress);
    }

    @Test
    public void findElementValue$ReplicationNodePort() {
        final String ReplicationNodePort = Helper.findElementValue("ReplicationNodePort", DCI);
        Assert.assertEquals("10000", ReplicationNodePort);
    }
}
