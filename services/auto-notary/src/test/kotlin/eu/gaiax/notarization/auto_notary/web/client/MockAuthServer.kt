package eu.gaiax.notarization.auto_notary.web.client

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 *
 * @author Neil Crossley
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class MockAuthServer
