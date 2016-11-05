package io.syncpoint.dtn;

import io.vertx.core.buffer.Buffer;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TManPduParserTest {
    private static final String T_OPEN_REQUEST = "00000022T1|123456789|987654321";
    private static final String T_DATA_REQUEST = "00000068T2|D1|{10000000000000000123|Friendly units moving|FRDNEU|Division|2}";


    @Test
    public void handlePdu$TOpenRequest() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        TmanPduParser parser = new TmanPduParser();
        parser.handler(pdu -> {
            Assert.assertEquals(TManPduType.T_OPNRQ, pdu.getPduType());
            latch.countDown();
        });

        parser.addData(Buffer.buffer("0000"));
        parser.addData(Buffer.buffer("0022T1|123"));
        parser.addData(Buffer.buffer("456789"));
        parser.addData(Buffer.buffer("|987654321"));

        latch.await(1L, TimeUnit.SECONDS);
        Assert.assertEquals(0L, latch.getCount());
    }

    @Test
    public void handlePdu$TData() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        TmanPduParser parser = new TmanPduParser();
        parser.handler(pdu -> {
            Assert.assertEquals(TManPduType.T_DATA, pdu.getPduType());
            Assert.assertEquals(T_DATA_REQUEST, pdu.getPdu().toString());
            latch.countDown();
        });

        parser.addData(Buffer.buffer(T_DATA_REQUEST));

        latch.await(1L, TimeUnit.SECONDS);
        Assert.assertEquals(0L, latch.getCount());

    }

    @Test
    public void handlePdu$TChkCon() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(2);
        TmanPduParser parser = new TmanPduParser();
        parser.handler(pdu -> {
            Assert.assertEquals(TManPduType.T_CHKCON, pdu.getPduType());
            latch.countDown();
        });

        parser.addData(Buffer.buffer("0000"));
        parser.addData(Buffer.buffer("0002T4000"));
        parser.addData(Buffer.buffer("00002T4"));

        latch.await(1L, TimeUnit.SECONDS);
        Assert.assertEquals(0L, latch.getCount());
    }

}
