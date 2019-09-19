// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.config.application.api;

import com.google.common.collect.ImmutableSet;
import com.yahoo.config.provision.Environment;
import com.yahoo.config.provision.RegionName;
import org.junit.Test;

import java.io.StringReader;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yahoo.config.application.api.Notifications.Role.author;
import static com.yahoo.config.application.api.Notifications.When.failing;
import static com.yahoo.config.application.api.Notifications.When.failingCommit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author bratseth
 */
public class DeploymentSpecTest {

    @Test
    public void testSpec() {
        String specXml = "<deployment version='1.0'>" +
                         "   <instances name='default'>" +
                         "      <test/>" +
                         "   </instances>" +
                         "</deployment>";

        StringReader r = new StringReader(specXml);
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals(specXml, spec.xmlForm());
        assertEquals(1, spec.steps().size());
        assertFalse(spec.majorVersion().isPresent());
        assertTrue(spec.steps().get(0).deploysTo(Environment.test));
        assertTrue(spec.includes(Environment.test, Optional.empty()));
        assertFalse(spec.includes(Environment.test, Optional.of(RegionName.from("region1"))));
        assertFalse(spec.includes(Environment.staging, Optional.empty()));
        assertFalse(spec.includes(Environment.prod, Optional.empty()));
        assertFalse(spec.globalServiceId().isPresent());
    }

    @Test
    public void testSpecPinningMajorVersion() {
        String specXml = "<deployment version='1.0' major-version='6'>" +
                         "   <instances name='default'>" +
                         "      <test/>" +
                         "   </instances>" +
                         "</deployment>";

        StringReader r = new StringReader(specXml);
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals(specXml, spec.xmlForm());
        assertEquals(1, spec.steps().size());
        assertTrue(spec.majorVersion().isPresent());
        assertEquals(6, (int)spec.majorVersion().get());
    }

    @Test
    public void stagingSpec() {
        StringReader r = new StringReader(
        "<deployment version='1.0'>" +
        "   <instances name='default'>" +
        "      <staging/>" +
        "   </instances>" +
        "</deployment>"
        );

        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals(2, spec.steps().size());
        assertTrue(spec.steps().get(0).deploysTo(Environment.test));
        assertTrue(spec.steps().get(1).deploysTo(Environment.staging));
        assertTrue(spec.includes(Environment.test, Optional.empty()));
        assertFalse(spec.includes(Environment.test, Optional.of(RegionName.from("region1"))));
        assertTrue(spec.includes(Environment.staging, Optional.empty()));
        assertFalse(spec.includes(Environment.prod, Optional.empty()));
        assertFalse(spec.globalServiceId().isPresent());
    }

    @Test
    public void minimalProductionSpec() {
        StringReader r = new StringReader(
                "<deployment version='1.0'>" +
                "   <instances name='default'>" +
                "      <prod>" +
                "         <region active='false'>us-east1</region>" +
                "         <region active='true'>us-west1</region>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );

        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals(4, spec.steps().size());

        assertTrue(spec.steps().get(0).deploysTo(Environment.test));

        assertTrue(spec.steps().get(1).deploysTo(Environment.staging));

        assertTrue(spec.steps().get(2).deploysTo(Environment.prod, Optional.of(RegionName.from("us-east1"))));
        assertFalse(((DeploymentSpec.DeclaredZone)spec.steps().get(2)).active());

        assertTrue(spec.steps().get(3).deploysTo(Environment.prod, Optional.of(RegionName.from("us-west1"))));
        assertTrue(((DeploymentSpec.DeclaredZone)spec.steps().get(3)).active());

        assertTrue(spec.includes(Environment.test, Optional.empty()));
        assertFalse(spec.includes(Environment.test, Optional.of(RegionName.from("region1"))));
        assertTrue(spec.includes(Environment.staging, Optional.empty()));
        assertTrue(spec.includes(Environment.prod, Optional.of(RegionName.from("us-east1"))));
        assertTrue(spec.includes(Environment.prod, Optional.of(RegionName.from("us-west1"))));
        assertFalse(spec.includes(Environment.prod, Optional.of(RegionName.from("no-such-region"))));
        assertFalse(spec.globalServiceId().isPresent());

        assertEquals(DeploymentSpec.UpgradePolicy.defaultPolicy, spec.upgradePolicy());
    }

    @Test
    public void maximalProductionSpec() {
        StringReader r = new StringReader(
        "<deployment version='1.0'>" +
        "   <instances name='default'>" +
        "      <test/>" +
        "      <staging/>" +
        "      <prod>" +
        "         <region active='false'>us-east1</region>" +
        "         <delay hours='3' minutes='30'/>" +
        "         <region active='true'>us-west1</region>" +
        "      </prod>" +
        "   </instances>" +
        "</deployment>"
        );

        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals(5, spec.steps().size());
        assertEquals(4, spec.zones().size());

        assertTrue(spec.steps().get(0).deploysTo(Environment.test));

        assertTrue(spec.steps().get(1).deploysTo(Environment.staging));

        assertTrue(spec.steps().get(2).deploysTo(Environment.prod, Optional.of(RegionName.from("us-east1"))));
        assertFalse(((DeploymentSpec.DeclaredZone)spec.steps().get(2)).active());

        assertTrue(spec.steps().get(3) instanceof DeploymentSpec.Delay);
        assertEquals(3 * 60 * 60 + 30 * 60, ((DeploymentSpec.Delay)spec.steps().get(3)).duration().getSeconds());

        assertTrue(spec.steps().get(4).deploysTo(Environment.prod, Optional.of(RegionName.from("us-west1"))));
        assertTrue(((DeploymentSpec.DeclaredZone)spec.steps().get(4)).active());

        assertTrue(spec.includes(Environment.test, Optional.empty()));
        assertFalse(spec.includes(Environment.test, Optional.of(RegionName.from("region1"))));
        assertTrue(spec.includes(Environment.staging, Optional.empty()));
        assertTrue(spec.includes(Environment.prod, Optional.of(RegionName.from("us-east1"))));
        assertTrue(spec.includes(Environment.prod, Optional.of(RegionName.from("us-west1"))));
        assertFalse(spec.includes(Environment.prod, Optional.of(RegionName.from("no-such-region"))));
        assertFalse(spec.globalServiceId().isPresent());
    }

    @Test
    public void productionSpecWithGlobalServiceId() {
        StringReader r = new StringReader(
            "<deployment version='1.0'>" +
            "   <instances name='default'>" +
            "      <prod global-service-id='query'>" +
            "         <region active='true'>us-east-1</region>" +
            "         <region active='true'>us-west-1</region>" +
            "      </prod>" +
            "   </instances>" +
            "</deployment>"
        );

        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals(spec.globalServiceId(), Optional.of("query"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void globalServiceIdInTest() {
        StringReader r = new StringReader(
                "<deployment version='1.0'>" +
                "   <instances name='default'>" +
                "      <test global-service-id='query' />" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
    }

    @Test(expected=IllegalArgumentException.class)
    public void globalServiceIdInStaging() {
        StringReader r = new StringReader(
                "<deployment version='1.0'>" +
                "   <instances name='default'>" +
                "      <staging global-service-id='query' />" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
    }

    @Test
    public void productionSpecWithGlobalServiceIdBeforeStaging() {
        StringReader r = new StringReader(
            "<deployment>" +
            "   <instances name='default'>" +
            "      <test/>" +
            "      <prod global-service-id='qrs'>" +
            "         <region active='true'>us-west-1</region>" +
            "         <region active='true'>us-central-1</region>" +
            "         <region active='true'>us-east-3</region>" +
            "      </prod>" +
            "      <staging/>" +
            "   </instances>" +
            "</deployment>"
        );

        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals("qrs", spec.globalServiceId().get());
    }

    @Test
    public void productionSpecWithUpgradePolicy() {
        StringReader r = new StringReader(
                "<deployment>" +
                "   <instances name='default'>" +
                "      <upgrade policy='canary'/>" +
                "      <prod>" +
                "         <region active='true'>us-west-1</region>" +
                "         <region active='true'>us-central-1</region>" +
                "         <region active='true'>us-east-3</region>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );

        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals("canary", spec.upgradePolicy().toString());
    }

    @Test
    public void maxDelayExceeded() {
        try {
            StringReader r = new StringReader(
                    "<deployment>" +
                    "   <instances name='default'>" +
                    "      <upgrade policy='canary'/>" +
                    "      <prod>" +
                    "         <region active='true'>us-west-1</region>" +
                    "         <delay hours='23'/>" +
                    "         <region active='true'>us-central-1</region>" +
                    "         <delay minutes='59' seconds='61'/>" +
                    "         <region active='true'>us-east-3</region>" +
                    "      </prod>" +
                    "   </instances>" +
                    "</deployment>"
            );
            DeploymentSpec.fromXml(r);
            fail("Expected exception due to exceeding the max total delay");
        }
        catch (IllegalArgumentException e) {
            // success
            assertEquals("The total delay specified is PT24H1S but max 24 hours is allowed", e.getMessage());
        }
    }

    @Test
    public void testEmpty() {
        assertFalse(DeploymentSpec.empty.globalServiceId().isPresent());
        assertEquals(DeploymentSpec.UpgradePolicy.defaultPolicy, DeploymentSpec.empty.upgradePolicy());
        assertTrue(DeploymentSpec.empty.steps().isEmpty());
        assertEquals("<deployment version='1.0'/>", DeploymentSpec.empty.xmlForm());
    }

    @Test
    public void productionSpecWithParallelDeployments() {
        StringReader r = new StringReader(
                "<deployment>" +
                "   <instances name='default'>" +
                "      <prod>" +
                "         <region active='true'>us-west-1</region>" +
                "         <parallel>" +
                "            <region active='true'>us-central-1</region>" +
                "            <region active='true'>us-east-3</region>" +
                "         </parallel>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        DeploymentSpec.ParallelZones parallelZones = ((DeploymentSpec.ParallelZones) spec.steps().get(3));
        assertEquals(2, parallelZones.zones().size());
        assertEquals(RegionName.from("us-central-1"), parallelZones.zones().get(0).region().get());
        assertEquals(RegionName.from("us-east-3"), parallelZones.zones().get(1).region().get());
    }

    @Test
    public void productionSpecWithDuplicateRegions() {
        StringReader r = new StringReader(
                "<deployment>" +
                "   <instances name='default'>" +
                "      <prod>" +
                "         <region active='true'>us-west-1</region>" +
                "         <parallel>" +
                "            <region active='true'>us-west-1</region>" +
                "             <region active='true'>us-central-1</region>" +
                "             <region active='true'>us-east-3</region>" +
                "         </parallel>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );
        try {
            DeploymentSpec.fromXml(r);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("prod.us-west-1 is listed twice in deployment.xml", e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void deploymentSpecWithIllegallyOrderedDeploymentSpec1() {
        StringReader r = new StringReader(
                "<deployment>" +
                "   <instances name='default'>" +
                "      <block-change days='sat' hours='10' time-zone='CET'/>" +
                "      <prod>" +
                "         <region active='true'>us-west-1</region>" +
                "      </prod>" +
                "      <block-change days='mon,tue' hours='15-16'/>" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deploymentSpecWithIllegallyOrderedDeploymentSpec2() {
        StringReader r = new StringReader(
                "<deployment>\n" +
                "   <instances name='default'>" +
                "      <block-change days='sat' hours='10' time-zone='CET'/>" +
                "      <test/>" +
                "      <prod>" +
                "         <region active='true'>us-west-1</region>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
    }

    @Test
    public void deploymentSpecWithChangeBlocker() {
        StringReader r = new StringReader(
                "<deployment>" +
                "   <instances name='default'>" +
                "      <block-change revision='false' days='mon,tue' hours='15-16'/>" +
                "      <block-change days='sat' hours='10' time-zone='CET'/>" +
                "      <prod>" +
                "         <region active='true'>us-west-1</region>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals(2, spec.changeBlocker().size());
        assertTrue(spec.changeBlocker().get(0).blocksVersions());
        assertFalse(spec.changeBlocker().get(0).blocksRevisions());
        assertEquals(ZoneId.of("UTC"), spec.changeBlocker().get(0).window().zone());

        assertTrue(spec.changeBlocker().get(1).blocksVersions());
        assertTrue(spec.changeBlocker().get(1).blocksRevisions());
        assertEquals(ZoneId.of("CET"), spec.changeBlocker().get(1).window().zone());

        assertTrue(spec.canUpgradeAt(Instant.parse("2017-09-18T14:15:30.00Z")));
        assertFalse(spec.canUpgradeAt(Instant.parse("2017-09-18T15:15:30.00Z")));
        assertFalse(spec.canUpgradeAt(Instant.parse("2017-09-18T16:15:30.00Z")));
        assertTrue(spec.canUpgradeAt(Instant.parse("2017-09-18T17:15:30.00Z")));

        assertTrue(spec.canUpgradeAt(Instant.parse("2017-09-23T09:15:30.00Z")));
        assertFalse(spec.canUpgradeAt(Instant.parse("2017-09-23T08:15:30.00Z"))); // 10 in CET
        assertTrue(spec.canUpgradeAt(Instant.parse("2017-09-23T10:15:30.00Z")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void athenz_config_is_disallowed_on_deployment_if_instances() {
        StringReader r = new StringReader(
                "<deployment athenz-domain='domain' athenz-service='service''>" +
                "   <instances name='default'>" +
                "      <prod>" +
                "         <region active='true'>us-west-1</region>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
    }

    @Test
    public void athenz_config_is_read_from_instance() {
        StringReader r = new StringReader(
                "<deployment'>" +
                "   <instances name='default' athenz-domain='domain' athenz-service='service'>" +
                "      <prod>" +
                "         <region active='true'>us-west-1</region>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals(spec.athenzDomain().get().value(), "domain");
        assertEquals(spec.athenzService(Environment.prod, RegionName.from("us-west-1")).get().value(), "service");
    }

    @Test
    public void athenz_service_is_overridden_from_environment() {
        StringReader r = new StringReader(
                "<deployment athenz-domain='domain' athenz-service='service'>" +
                "   <instances name='default' athenz-domain='domain' athenz-service='service'>" +
                "      <test/>" +
                "      <prod athenz-service='prod-service'>" +
                "         <region active='true'>us-west-1</region>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
        assertEquals(spec.athenzDomain().get().value(), "domain");
        assertEquals(spec.athenzService(Environment.prod, RegionName.from("us-west-1")).get().value(), "prod-service");
    }

    @Test(expected = IllegalArgumentException.class)
    public void it_fails_when_athenz_service_is_not_defined() {
        StringReader r = new StringReader(
                "<deployment>" +
                "   <instances name='default' athenz-domain='domain'>" +
                "      <prod>" +
                "         <region active='true'>us-west-1</region>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
    }

    @Test(expected = IllegalArgumentException.class)
    public void it_fails_when_athenz_service_is_configured_but_not_athenz_domain() {
        StringReader r = new StringReader(
                "<deployment>" +
                "   <instances name='default'>" +
                "      <prod athenz-service='service'>" +
                "         <region active='true'>us-west-1</region>" +
                "      </prod>" +
                "   </instances>" +
                "</deployment>"
        );
        DeploymentSpec spec = DeploymentSpec.fromXml(r);
    }

    @Test
    public void noNotifications() {
        assertEquals(Notifications.none(),
                     DeploymentSpec.fromXml("<deployment>" +
                                            "   <instances name='default'/>" +
                                            "</deployment>").notifications());
    }

    @Test
    public void emptyNotifications() {
        DeploymentSpec spec = DeploymentSpec.fromXml("<deployment>" +
                                                     "   <instances name='default'>" +
                                                     "      <notifications/>" +
                                                     "   </instances>" +
                                                     "</deployment>");
        assertEquals(Notifications.none(),
                     spec.notifications());
    }

    @Test
    public void someNotifications() {
        DeploymentSpec spec = DeploymentSpec.fromXml("<deployment>\n" +
                                                     "   <instances name='default'>" +
                                                     "      <notifications when=\"failing\">" +
                                                     "         <email role=\"author\"/>" +
                                                     "         <email address=\"john@dev\" when=\"failing-commit\"/>" +
                                                     "         <email address=\"jane@dev\"/>" +
                                                     "      </notifications>" +
                                                     "   </instances>" +
                                                     "</deployment>");
        assertEquals(ImmutableSet.of(author), spec.notifications().emailRolesFor(failing));
        assertEquals(ImmutableSet.of(author), spec.notifications().emailRolesFor(failingCommit));
        assertEquals(ImmutableSet.of("john@dev", "jane@dev"), spec.notifications().emailAddressesFor(failingCommit));
        assertEquals(ImmutableSet.of("jane@dev"), spec.notifications().emailAddressesFor(failing));
    }

    @Test
    public void customTesterFlavor() {
        DeploymentSpec spec = DeploymentSpec.fromXml("<deployment>" +
                                                     "   <instances name='default'>" +
                                                     "      <test tester-flavor=\"d-1-4-20\" />" +
                                                     "      <prod tester-flavor=\"d-2-8-50\">" +
                                                     "         <region active=\"false\">us-north-7</region>" +
                                                     "      </prod>" +
                                                     "   </instances>" +
                                                     "</deployment>");
        assertEquals(Optional.of("d-1-4-20"), spec.steps().get(0).zones().get(0).testerFlavor());
        assertEquals(Optional.empty(), spec.steps().get(1).zones().get(0).testerFlavor());
        assertEquals(Optional.of("d-2-8-50"), spec.steps().get(2).zones().get(0).testerFlavor());
    }

    @Test
    public void noEndpoints() {
        assertEquals(Collections.emptyList(),
                     DeploymentSpec.fromXml("<deployment>" +
                                            "   <instances name='default'/>" +
                                            "</deployment>").endpoints());
    }

    @Test
    public void emptyEndpoints() {
        var spec = DeploymentSpec.fromXml("<deployment>" +
                                          "   <instances name='default'>" +
                                          "      <endpoints/>" +
                                          "   </instances>" +
                                          "</deployment>");
        assertEquals(Collections.emptyList(), spec.endpoints());
    }

    @Test
    public void someEndpoints() {
        var spec = DeploymentSpec.fromXml("" +
                                          "<deployment>" +
                                          "   <instances name='default'>" +
                                          "      <prod>" +
                                          "         <region active=\"true\">us-east</region>" +
                                          "      </prod>" +
                                          "      <endpoints>" +
                                          "         <endpoint id=\"foo\" container-id=\"bar\">" +
                                          "            <region>us-east</region>" +
                                          "         </endpoint>" +
                                          "         <endpoint id=\"nalle\" container-id=\"frosk\" />" +
                                          "         <endpoint container-id=\"quux\" />" +
                                          "      </endpoints>" +
                                          "   </instances>" +
                                          "</deployment>");

        assertEquals(
                List.of("foo", "nalle", "default"),
                spec.endpoints().stream().map(Endpoint::endpointId).collect(Collectors.toList())
        );

        assertEquals(
                List.of("bar", "frosk", "quux"),
                spec.endpoints().stream().map(Endpoint::containerId).collect(Collectors.toList())
        );

        assertEquals(Set.of(RegionName.from("us-east")), spec.endpoints().get(0).regions());
    }

    @Test
    public void invalidEndpoints() {
        assertInvalid("<endpoint id='FOO' container-id='qrs'/>"); // Uppercase
        assertInvalid("<endpoint id='123' container-id='qrs'/>"); // Starting with non-character
        assertInvalid("<endpoint id='foo!' container-id='qrs'/>"); // Non-alphanumeric
        assertInvalid("<endpoint id='foo.bar' container-id='qrs'/>");
        assertInvalid("<endpoint id='foo--bar' container-id='qrs'/>"); // Multiple consecutive dashes
        assertInvalid("<endpoint id='foo-' container-id='qrs'/>"); // Trailing dash
        assertInvalid("<endpoint id='foooooooooooo' container-id='qrs'/>"); // Too long
        assertInvalid("<endpoint id='foo' container-id='qrs'/><endpoint id='foo' container-id='qrs'/>"); // Duplicate
    }

    @Test
    public void validEndpoints() {
        assertEquals(List.of("default"), endpointIds("<endpoint container-id='qrs'/>"));
        assertEquals(List.of("default"), endpointIds("<endpoint id='' container-id='qrs'/>"));
        assertEquals(List.of("f"), endpointIds("<endpoint id='f' container-id='qrs'/>"));
        assertEquals(List.of("foo"), endpointIds("<endpoint id='foo' container-id='qrs'/>"));
        assertEquals(List.of("foo-bar"), endpointIds("<endpoint id='foo-bar' container-id='qrs'/>"));
        assertEquals(List.of("foo", "bar"), endpointIds("<endpoint id='foo' container-id='qrs'/><endpoint id='bar' container-id='qrs'/>"));
        assertEquals(List.of("fooooooooooo"), endpointIds("<endpoint id='fooooooooooo' container-id='qrs'/>"));
    }

    @Test
    public void endpointDefaultRegions() {
        var spec = DeploymentSpec.fromXml("" +
                                          "<deployment>" +
                                          "   <instances name='default'>" +
                                          "      <prod>" +
                                          "         <region active=\"true\">us-east</region>" +
                                          "         <region active=\"true\">us-west</region>" +
                                          "      </prod>" +
                                          "      <endpoints>" +
                                          "         <endpoint id=\"foo\" container-id=\"bar\">" +
                                          "         <region>us-east</region>" +
                                          "      </endpoint>" +
                                          "      <endpoint id=\"nalle\" container-id=\"frosk\" />" +
                                          "         <endpoint container-id=\"quux\" />" +
                                          "      </endpoints>" +
                                          "   </instances>" +
                                          "</deployment>");

        assertEquals(Set.of("us-east"), endpointRegions("foo", spec));
        assertEquals(Set.of("us-east", "us-west"), endpointRegions("nalle", spec));
        assertEquals(Set.of("us-east", "us-west"), endpointRegions("default", spec));
    }

    private static void assertInvalid(String endpointTag) {
        try {
            endpointIds(endpointTag);
            fail("Expected exception for input '" + endpointTag + "'");
        } catch (IllegalArgumentException ignored) {}
    }

    private static Set<String> endpointRegions(String endpointId, DeploymentSpec spec) {
        return spec.endpoints().stream()
                .filter(endpoint -> endpoint.endpointId().equals(endpointId))
                .flatMap(endpoint -> endpoint.regions().stream())
                .map(RegionName::value)
                .collect(Collectors.toSet());
    }

    private static List<String> endpointIds(String endpointTag) {
        var xml = "<deployment>" +
                  "   <instances name='default'>" +
                  "      <prod>" +
                  "         <region active=\"true\">us-east</region>" +
                  "      </prod>" +
                  "      <endpoints>" +
                  endpointTag +
                  "      </endpoints>" +
                  "   </instances>" +
                  "</deployment>";

        return DeploymentSpec.fromXml(xml).endpoints().stream()
                             .map(Endpoint::endpointId)
                             .collect(Collectors.toList());
    }

}
