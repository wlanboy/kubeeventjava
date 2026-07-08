package com.example.kubeevent.daten;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class EventKindTest {

	@ParameterizedTest
	@EnumSource(EventKind.class)
	void fromK8sKind_resolvesEveryDeclaredKind(EventKind kind) {
		assertThat(EventKind.fromK8sKind(kind.k8sKind)).isEqualTo(kind);
	}

	@ParameterizedTest
	@ValueSource(strings = {"Secret", "ConfigMap", "Service", ""})
	void fromK8sKind_returnsNullForUnknownKind(String kind) {
		assertThat(EventKind.fromK8sKind(kind)).isNull();
	}

	@Test
	void fromK8sKind_returnsNullForNullKind() {
		assertThat(EventKind.fromK8sKind(null)).isNull();
	}

	@Test
	void tags_namespacedWithoutReason_omitsReasonTag() {
		String[] tags = EventKind.POD.tags("ns", "mypod", "Warning", "OOMKilled");

		assertThat(tags).containsExactly(
				"namespace", "ns",
				"pod", "mypod",
				"type", "Warning");
	}

	@Test
	void tags_namespacedWithReason_includesReasonTag() {
		String[] tags = EventKind.DEPLOYMENT.tags("ns", "mydeploy", "Warning", "FailedCreate");

		assertThat(tags).containsExactly(
				"namespace", "ns",
				"deployment", "mydeploy",
				"type", "Warning",
				"reason", "FailedCreate");
	}

	@Test
	void tags_notNamespacedWithReason_omitsNamespaceTag() {
		String[] tags = EventKind.NODE.tags("ns", "node1", "Normal", "NodeReady");

		assertThat(tags).containsExactly(
				"node", "node1",
				"type", "Normal",
				"reason", "NodeReady");
	}

	@Test
	void tags_persistentVolume_notNamespacedButIncludesReason() {
		String[] tags = EventKind.PERSISTENT_VOLUME.tags("ns", "pv1", "Warning", "FailedMount");

		assertThat(tags).containsExactly(
				"pv", "pv1",
				"type", "Warning",
				"reason", "FailedMount");
	}
}
