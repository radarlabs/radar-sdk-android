package io.radar.mvnpublish

enum MavenServer {

    STAGING('staging'),
    RELEASE('release');

    final String url

    MavenServer(String name) {
        url = "https://s01.oss.sonatype.org/service/local/$name/deploy/maven2"
    }
}
