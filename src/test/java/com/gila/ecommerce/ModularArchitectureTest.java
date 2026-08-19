package com.gila.ecommerce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularArchitectureTest {

	private final ApplicationModules modules = ApplicationModules.of(EcommerceApplication.class);

	@Test
	void discoversExpectedModules() {
		var moduleNames = modules.stream()
				.map(module -> module.getIdentifier().toString())
				.collect(Collectors.toSet());

		assertEquals(Set.of("catalog", "ordering", "payment", "productimport", "shared"), moduleNames);
	}

	@Test
	void verifiesModuleBoundaries() {
		modules.verify();
	}

}
