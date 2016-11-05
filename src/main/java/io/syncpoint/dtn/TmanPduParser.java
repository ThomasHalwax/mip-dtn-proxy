package io.syncpoint.dtn;

import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public final class TmanPduParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(TmanPduParser.class);
    private static final int PDU_SIZE_LENGTH = 8;

    private Consumer<TManPdu> pduHandler = defaultConsumer();
    private Consumer<Throwable> errorHandler = defaultErrorHandler();
    private Consumer<Buffer> parser = pduLengthParser();

    private Buffer buffer = Buffer.buffer();
    private int currentPduLength = 0;

    public void addData(Buffer data) {
        buffer.appendBuffer(data);
        parser.accept(buffer);
    }

    public void handler(Consumer<TManPdu> handler) {
        this.pduHandler = handler;
    }
    public void errorHandler(Consumer<Throwable> errorHandler) { this.errorHandler = errorHandler; }

    private Consumer<Buffer> pduLengthParser() {
        return dataAvailable -> {
            if (dataAvailable.length() >= PDU_SIZE_LENGTH) {
                try {
                    currentPduLength = PDU_SIZE_LENGTH + Integer.parseInt(dataAvailable.getString(0, PDU_SIZE_LENGTH));
                }
                catch (NumberFormatException numberEx) {
                    errorHandler.accept(numberEx);
                    return;
                }
                parser = pduPayloadParser();
                parser.accept(dataAvailable);
            }
        };
    }

    private Consumer<Buffer> pduPayloadParser() {
        return dataAvailable -> {
            if (dataAvailable.length() >= currentPduLength) {
                Buffer pdu = dataAvailable.getBuffer(0, currentPduLength);

                if (buffer.length() > pdu.length()) {
                    buffer = buffer.getBuffer(currentPduLength, buffer.length());
                }
                else {
                    buffer = Buffer.buffer();
                }

                pduHandler.accept(new TManPdu(pdu));
                currentPduLength = 0;
                parser = pduLengthParser();
                parser.accept(buffer);
            }
        };
    }

    // default consumers
    private Consumer<Throwable> defaultErrorHandler() {
        return throwable -> LOGGER.error("error parsing TMAN PDU", throwable);
    }

    private Consumer<TManPdu> defaultConsumer() {
        return buffer -> LOGGER.info("DEFAULT CONSUMER WITHOUT FUNCTIONALITY");
    }
}
