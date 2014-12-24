/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package geogebra.linker;

import java.util.Arrays;
import java.util.Date;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.impl.SelectionInformation;

/**
 * AppCacheLinker - linker for public path resources in the Application Cache.
 * <p>
 * To use:
 * <ol>
 * <li>Add {@code manifest="YOURMODULENAME/appcache.nocache.manifest"} to the
 * {@code <html>} tag in your base html file. E.g.,
 * {@code <html manifest="mymodule/appcache.nocache.manifest">}</li>
 * <li>Add a mime-mapping to your web.xml file:
 * <p>
 * 
 * <pre>
 * {@code <mime-mapping>
 * <extension>manifest</extension>
 * <mime-type>text/cache-manifest</mime-type>
 * </mime-mapping>
 * }
 * </pre>
 * 
 * </li>
 * </ol>
 * <p>
 * On every compile, this linker will regenerate the appcache.nocache.manifest
 * file with files from the public path of your module.
 * <p>
 * To obtain a manifest that contains other files in addition to those generated
 * by this linker, create a class that inherits from this one and overrides
 * {@code otherCachedFiles()}, and use it as a linker instead:
 * <p>
 * 
 * <pre>
 * <blockquote>
 * {@code @Shardable}
 * public class MyAppCacheLinker extends AbstractAppCacheLinker {
 *   {@code @Override}
 *   protected String[] otherCachedFiles() {
 *     return new String[] {"/MyApp.html","/MyApp.css"};
 *   }
 * }
 * </blockquote>
 * </pre>
 */
@LinkerOrder(Order.POST)
public class SimpleAppCacheLinker extends AbstractLinker {

	private static final String MANIFEST = "appcache.nocache.manifest";

	@Override
	public String getDescription() {
		return "AppCacheLinker";
	}

	@Override
	public ArtifactSet link(TreeLogger logger, LinkerContext context,
	        ArtifactSet artifacts, boolean onePermutation)
	        throws UnableToCompleteException {

		ArtifactSet toReturn = new ArtifactSet(artifacts);

		if (onePermutation) {
			return toReturn;
		}

		if (toReturn.find(SelectionInformation.class).isEmpty()) {
			logger.log(TreeLogger.INFO, "devmode: generating empty " + MANIFEST);
			artifacts = null;
		}

		// Create the general cache-manifest resource for the landing page:
		toReturn.add(emitLandingPageCacheManifest(context, logger, artifacts));
		return toReturn;
	}

	/**
	 * Override this method to force the linker to also include more files in
	 * the manifest.
	 */
	protected String[] otherCachedFiles() {
		return null;
	}

	/**
	 * Creates the cache-manifest resource specific for the landing page.
	 * 
	 * @param context
	 *            the linker environment
	 * @param logger
	 *            the tree logger to record to
	 * @param artifacts
	 *            {@code null} to generate an empty cache manifest
	 */
	private Artifact<?> emitLandingPageCacheManifest(LinkerContext context,
	        TreeLogger logger, ArtifactSet artifacts)
	        throws UnableToCompleteException {
		StringBuilder publicSourcesSb = new StringBuilder();
		StringBuilder staticResoucesSb = new StringBuilder();

		if (artifacts != null) {
			// Iterate over all emitted artifacts, and collect all cacheable
			// artifacts
			for (@SuppressWarnings("rawtypes")
			Artifact artifact : artifacts) {
				if (artifact instanceof EmittedArtifact) {
					EmittedArtifact ea = (EmittedArtifact) artifact;
					String pathName = ea.getPartialPath();
					if (pathName.endsWith("symbolMap")
					        || pathName.endsWith(".xml.gz")
					        || pathName.endsWith("rpc.log")
					        || pathName.endsWith("gwt.rpc")
					        || pathName.endsWith("manifest.txt")
					        || pathName.startsWith("rpcPolicyManifest")
					        || pathName.endsWith("cssmap")
					        || pathName.endsWith("svnignore.txt")
					        || pathName.endsWith("compilation-mappings.txt")
					        || pathName.endsWith(".php")
					        || pathName.endsWith("README")
					        || pathName.endsWith("oauthWindow.html")
					        || pathName.endsWith("windowslive.html")
					        || pathName.endsWith("devmode.js")
					        || pathName.startsWith("js/properties_")) {
						// skip these resources
					} else {
						publicSourcesSb.append(pathName + "\n");
					}
				}
			}

			String[] cacheExtraFiles = getCacheExtraFiles();
			for (int i = 0; i < cacheExtraFiles.length; i++) {
				staticResoucesSb.append(cacheExtraFiles[i]);
				staticResoucesSb.append("\n");
			}
		}

		// build cache list
		StringBuilder sb = new StringBuilder();
		sb.append("CACHE MANIFEST\n");
		sb.append("# Unique id #" + (new Date()).getTime() + "."
		        + Math.random() + "\n");
		// we have to generate this unique id because the resources can change
		// but
		// the hashed cache.html files can remain the same.
		sb.append("# Note: must change this every time for cache to invalidate\n");
		sb.append("\n");
		sb.append("CACHE:\n");
		sb.append("# Static app files\n");
		sb.append(staticResoucesSb.toString());
		sb.append("\n# Generated app files\n");
		sb.append(publicSourcesSb.toString());
		sb.append("\n\n");
		sb.append("# All other resources require the user to be online.\n");
		sb.append("NETWORK:\n");
		sb.append("*\n");

		logger.log(
		        TreeLogger.INFO,
		        "Make sure you have the following"
		                + " attribute added to your landing page's <html> tag: <html manifest=\""
		                + context.getModuleFunctionName() + "/" + MANIFEST
		                + "\">");

		// Create the manifest as a new artifact and return it:
		return emitString(logger, sb.toString(), MANIFEST);
	}

	/**
	 * Obtains the extra files to include in the manifest. Ensures the returned
	 * array is not null.
	 */
	private String[] getCacheExtraFiles() {
		String[] cacheExtraFiles = otherCachedFiles();
		return cacheExtraFiles == null ? new String[0] : Arrays.copyOf(
		        cacheExtraFiles, cacheExtraFiles.length);
	}
}
