package io.syncpoint.dtn;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class DtnUri {
    public static final String SCHEMA_AND_PREFIX = "dtn:/";
    private final String uri;

    private DtnUri(String host, String application, String process) {
        List<String> urlChunks = new ArrayList<>();
        urlChunks.add(SCHEMA_AND_PREFIX);
        if (host != null) urlChunks.add(Helper.removeLeadingSlash(host));
        if (application != null) urlChunks.add(Helper.removeLeadingSlash(application));
        if (process != null) {
            process = Helper.removeLeadingSlash(process);
            if (process.startsWith(application)) {
                process = process.substring(application.length(), process.length());
                process = Helper.removeLeadingSlash(process);
            }
            urlChunks.add(process);
        }

        StringJoiner joiner = new StringJoiner("/");
        urlChunks.forEach(joiner::add);
        this.uri = joiner.toString();
    }

    @Override
    public String toString() {
        return this.uri;
    }

    public static DtnUriBuilder builder() {
        return new DtnUriBuilder();
    }

    public static class DtnUriBuilder {
        private String host;
        private String application;
        private String process;

        public DtnUriBuilder host(String host) {
            this.host = host;
            return this;
        }

        public DtnUriBuilder application(String application) {
            this.application = application;
            return this;
        }

        public DtnUriBuilder process(String process) {
            this.process = process;
            return this;
        }

        public DtnUri build() {
            return new DtnUri(this.host, this.application, this.process);
        }
    }


}
