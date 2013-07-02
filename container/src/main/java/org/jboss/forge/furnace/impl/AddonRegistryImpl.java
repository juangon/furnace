/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonFilter;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.addons.AddonStatus;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.services.ExportedInstance;
import org.jboss.forge.furnace.services.ServiceRegistry;
import org.jboss.forge.furnace.util.AddonFilters;
import org.jboss.forge.furnace.util.Assert;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonRegistryImpl implements AddonRegistry
{
   private static final Logger logger = Logger.getLogger(AddonRegistryImpl.class.getName());

   private final LockManager lock;
   private List<AddonRepository> repositories;

   private AddonLifecycleManager manager;

   public AddonRegistryImpl(LockManager lock, AddonLifecycleManager manager, List<AddonRepository> repositories)
   {
      Assert.notNull(lock, "LockManager must not be null.");
      Assert.notNull(manager, "Addon lifecycle manager must not be null.");
      Assert.notNull(repositories, "AddonRepository list must not be null.");
      Assert.isTrue(repositories.size() > 0, "AddonRepository list must not be empty.");

      this.lock = lock;
      this.manager = manager;
      this.repositories = repositories;

      logger.log(Level.FINE, "Instantiated AddonRegistryImpl: " + this);
   }
   
   @Override
   public void dispose()
   {
      manager.dispose(this);
   }

   @Override
   public Addon getAddon(final AddonId id)
   {
      Assert.notNull(id, "AddonId must not be null.");
      return lock.performLocked(LockMode.READ, new Callable<Addon>()
      {
         @Override
         public Addon call() throws Exception
         {
            Addon result = null;
            for (Addon addon : getAddons())
            {
               if (id.equals(addon.getId()))
               {
                  result = addon;
               }
            }

            if (result == null)
            {
               result = manager.getAddon(id);
            }

            return result;
         }
      });
   }

   @Override
   public Set<Addon> getAddons()
   {
      return getAddons(AddonFilters.all());
   }

   @Override
   public Set<Addon> getAddons(final AddonFilter filter)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<Addon>>()
      {
         @Override
         public Set<Addon> call() throws Exception
         {
            HashSet<Addon> result = new HashSet<Addon>();

            for (Addon addon : manager.getAddons(new AddonRepositoryFilter(getRepositories())))
            {
               if (filter.accept(addon))
                  result.add(addon);
            }

            return result;
         }
      });
   }

   @Override
   public Set<AddonRepository> getRepositories()
   {
      return Collections.unmodifiableSet(new LinkedHashSet<AddonRepository>(repositories));
   }

   @Override
   public <T> Set<ExportedInstance<T>> getExportedInstances(final Class<T> type)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<ExportedInstance<T>>>()
      {
         @Override
         public Set<ExportedInstance<T>> call() throws Exception
         {
            Set<ExportedInstance<T>> result = new HashSet<ExportedInstance<T>>();
            for (Addon addon : getAddons())
            {
               if (AddonStatus.STARTED.equals(addon.getStatus()))
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  result.addAll(serviceRegistry.getExportedInstances(type));
               }
            }
            return result;
         }
      });
   }

   @Override
   public <T> Set<ExportedInstance<T>> getExportedInstances(final String type)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<ExportedInstance<T>>>()
      {
         @Override
         public Set<ExportedInstance<T>> call() throws Exception
         {
            Set<ExportedInstance<T>> result = new HashSet<ExportedInstance<T>>();
            for (Addon addon : getAddons())
            {
               if (addon.getStatus().isStarted())
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  Set<ExportedInstance<T>> remoteInstances = serviceRegistry.getExportedInstances(type);
                  result.addAll(remoteInstances);
               }
            }
            return result;
         }
      });
   }

   @Override
   public <T> ExportedInstance<T> getExportedInstance(final Class<T> type)
   {
      return lock.performLocked(LockMode.READ, new Callable<ExportedInstance<T>>()
      {
         @Override
         public ExportedInstance<T> call() throws Exception
         {
            ExportedInstance<T> result = null;
            for (Addon addon : getAddons())
            {
               if (addon.getStatus().isStarted())
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  result = serviceRegistry.getExportedInstance(type);
                  if (result != null)
                  {
                     break;
                  }
               }
            }
            return result;
         }
      });
   }

   @Override
   public <T> ExportedInstance<T> getExportedInstance(final String type)
   {
      return lock.performLocked(LockMode.READ, new Callable<ExportedInstance<T>>()
      {
         @Override
         public ExportedInstance<T> call() throws Exception
         {
            ExportedInstance<T> result = null;
            for (Addon addon : getAddons())
            {
               if (addon.getStatus().isStarted())
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  result = serviceRegistry.getExportedInstance(type);
                  if (result != null)
                  {
                     break;
                  }
               }
            }
            return result;
         }
      });
   }

   @Override
   public Set<Class<?>> getExportedTypes()
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<Class<?>>>()
      {
         @Override
         public Set<Class<?>> call() throws Exception
         {
            Set<Class<?>> result = new HashSet<Class<?>>();
            for (Addon addon : getAddons())
            {
               if (AddonStatus.STARTED.equals(addon.getStatus()))
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  result.addAll(serviceRegistry.getExportedTypes());
               }
            }
            return result;
         }
      });
   }

   @Override
   public <T> Set<Class<T>> getExportedTypes(final Class<T> type)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<Class<T>>>()
      {
         @Override
         public Set<Class<T>> call() throws Exception
         {
            Set<Class<T>> result = new HashSet<Class<T>>();
            for (Addon addon : getAddons())
            {
               if (AddonStatus.STARTED.equals(addon.getStatus()))
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  result.addAll(serviceRegistry.getExportedTypes(type));
               }
            }
            return result;
         }
      });
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("REPOSITORIES:").append("\n");

      Iterator<AddonRepository> repostioryIterator = getRepositories().iterator();
      while (repostioryIterator.hasNext())
      {
         AddonRepository addon = repostioryIterator.next();
         builder.append(addon.toString());
         if (repostioryIterator.hasNext())
            builder.append("\n");
      }

      builder.append("\n");
      builder.append("\n");
      builder.append("ADDONS:").append("\n");

      Iterator<Addon> addonIterator = getAddons().iterator();
      while (addonIterator.hasNext())
      {
         Addon addon = addonIterator.next();
         builder.append(addon.toString());
         if (addonIterator.hasNext())
            builder.append("\n");
      }

      return builder.toString();
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((repositories == null) ? 0 : repositories.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AddonRegistryImpl other = (AddonRegistryImpl) obj;
      if (repositories == null)
      {
         if (other.repositories != null)
            return false;
      }
      else if (!repositories.equals(other.repositories))
         return false;
      return true;
   }
}
