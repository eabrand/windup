package org.jboss.windup.engine.visitor;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.jboss.windup.engine.visitor.base.EmptyGraphVisitor;
import org.jboss.windup.graph.dao.ArchiveDaoBean;
import org.jboss.windup.graph.dao.ArchiveEntryDaoBean;
import org.jboss.windup.graph.dao.FileResourceDaoBean;
import org.jboss.windup.graph.model.resource.ArchiveEntryResource;
import org.jboss.windup.graph.model.resource.ArchiveResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Vertex;

/**
 * Goes through an archive adding the archive entries to the graph.
 * 
 * @author bradsdavis@gmail.com
 *
 */
public class ArchiveEntryIndexVisitor extends EmptyGraphVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ArchiveEntryIndexVisitor.class);

	@Inject
	FileResourceDaoBean fileDao;
	
	@Inject
	private ArchiveDaoBean archiveDao;

	@Inject
	private ArchiveEntryDaoBean archiveEntryDao;
	
	@Override
	public void run() {
		final int total = (int)archiveDao.count(archiveDao.getAll());

		int i=1;
		for(final ArchiveResource archive : archiveDao.getAll()) {
			visitArchive(archive); 
			LOG.info("Processed: "+i+" of "+total+" Archives.");
			i++;
		}
		
		final int finalTotal = (int)archiveDao.count(archiveDao.getAll());
		LOG.info("Total: "+finalTotal+" Archives.");
	}
	
	@Override
	public void visitArchive(ArchiveResource result) {
		Vertex v = result.asVertex();
		ArchiveResource file = archiveDao.getById(v.getId());
		
		ZipFile zipFileReference = null;
		try {
			File zipFile = result.asFile();
			zipFileReference = new ZipFile(zipFile);
			Enumeration<? extends ZipEntry> entries = zipFileReference.entries();
			
			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if(entry.isDirectory()) {
					continue;
				}
				//creates a new archive entry.
				ArchiveEntryResource resource = archiveEntryDao.create(null);
				resource.setArchiveEntry(entry.getName());
				resource.setArchive(file);
				
				//now look for recursive archives...
				if(endsWithExtension(entry.getName())) {
					String subArchiveName = entry.getName();
					if(StringUtils.contains(entry.getName(), "/")) {
						subArchiveName = StringUtils.substringAfterLast(subArchiveName, "/");
					}
					LOG.info("Found nested archive: "+subArchiveName);
					
					//create an archive record, and process it.
					ArchiveResource subArchive = archiveDao.create();
					subArchive.setArchiveName(subArchiveName);
					subArchive.setResource(resource);
					result.addChildArchive(subArchive);
					
					visitArchive(subArchive);
				}
				
			}
		} catch (IOException e) {
			LOG.error("Exception while reading JAR.", e);
		}
		finally {
			org.apache.commons.io.IOUtils.closeQuietly(zipFileReference);
			archiveDao.commit();
		}
	}
	

	private boolean endsWithExtension(String path) {
		for(String extension : getZipExtensions()) {
			if(StringUtils.endsWith(path, extension)) {
				return true;
			}
		}
		return false;
	}
	
	private Set<String> getZipExtensions() {
		Set<String> extensions = new HashSet<String>();
		extensions.add(".war");
		extensions.add(".ear");
		extensions.add(".jar");
		extensions.add(".sar");
		extensions.add(".rar");
		
		return extensions;
	}
}