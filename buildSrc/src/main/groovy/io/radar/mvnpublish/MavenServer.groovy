package io.radar.mvnpublish

enum MavenServer {

    STAGING('service/local/staging/deploy/maven2'),
    RELEASE('service/local/release/deploy/maven2'),
    SNAPSHOT('content/repositories/snapshots');

    final String url

    MavenServer(String path) {
        url = "https://s01.oss.sonatype.org/$path"
    }
}
