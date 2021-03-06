/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.proxy.classloader;

import java.io.File;
import java.io.PrintStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.classloader.mock.CustomPrintStream;
import org.jboss.forge.classloader.mock.CustomPrintStreamFactory;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.proxy.Proxies;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ClassLoaderAdapterJavaIOSubclassTest
{
   @Deployment(order = 3)
   public static AddonArchive getDeployment()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addBeansXML()
               .addClasses(CustomPrintStreamFactory.class)
               .addClasses(CustomPrintStream.class)
               .addAsLocalServices(ClassLoaderAdapterJavaIOSubclassTest.class);

      return archive;
   }

   @Deployment(name = "dep,1", testable = false, order = 2)
   public static AddonArchive getDeploymentDep1()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addClasses(CustomPrintStreamFactory.class)
               .addClasses(CustomPrintStream.class)
               .addBeansXML();

      return archive;
   }

   @Test
   public void testSubclassedPrintStream() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();
      ClassLoader thisLoader = ClassLoaderAdapterJavaIOSubclassTest.class.getClassLoader();
      ClassLoader dep1Loader = registry.getAddon(AddonId.from("dep", "1")).getClassLoader();

      Class<?> foreignType = dep1Loader.loadClass(CustomPrintStreamFactory.class.getName());
      try
      {
         @SuppressWarnings({ "unused" })
         CustomPrintStream custom = (CustomPrintStream) foreignType.getMethod("getPrintStream")
                  .invoke(foreignType.newInstance());

         Assert.fail("Should have received a classcast exception");
      }
      catch (ClassCastException e)
      {
      }

      Object delegate = foreignType.newInstance();
      CustomPrintStreamFactory enhancedFactory = (CustomPrintStreamFactory) ClassLoaderAdapterBuilder
               .callingLoader(thisLoader)
               .delegateLoader(dep1Loader).enhance(delegate);

      Assert.assertTrue(Proxies.isForgeProxy(enhancedFactory));
      PrintStream result = enhancedFactory.getPrintStream();
      Assert.assertFalse(Proxies.isForgeProxy(result));

      File file = File.createTempFile("furnace", "printStream");
      file.deleteOnExit();
      enhancedFactory.usePrintStream(new CustomPrintStream(file));

      try
      {
         @SuppressWarnings("unused")
         CustomPrintStream customResult = enhancedFactory.getCustomPrintStream();
         Assert.fail("Should have received a classcast exception");
      }
      catch (ClassCastException e)
      {
      }
   }
}
