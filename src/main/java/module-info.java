open module it.auties.whatsapp4j {
    requires static lombok;
    requires static jdk.compiler;
    requires transitive java.logging;
    requires transitive java.desktop;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires java.prefs;
    requires org.bouncycastle.provider;
    requires it.auties.protoc.api;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires io.github.classgraph;
    requires java.net.http;
    requires jdk.crypto.ec;
    requires signal.protocol.java;
    requires curve25519.java;
    requires netty.buffer;
}