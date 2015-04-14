package nl.tudelft.ewi.build.extensions.staging;

import java.io.File;
import java.io.IOException;

import nl.tudelft.ewi.build.docker.Logger;

import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.jaxrs.models.GitSource;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

@Slf4j
public class GitStagingDirectoryPreparer implements StagingDirectoryPreparer<GitSource> {

	public void prepareStagingDirectory(GitSource source, Logger logger, File stagingDirectory) throws IOException {
		Git git = cloneRepository(source, logger, stagingDirectory);
		checkoutCommit(source, logger, git);
	}

	private Git cloneRepository(GitSource source, Logger logger, File stagingDirectory) throws IOException {
		try {
			log.info("Cloning from repository: {}", source.getRepositoryUrl());
			CloneCommand clone = Git.cloneRepository();
			clone.setBare(false);
			clone.setDirectory(stagingDirectory);
            clone.setCloneAllBranches(true);
            clone.setURI(source.getRepositoryUrl());
            return clone.call();
		}
		catch (GitAPIException e) {
			logger.onNextLine("[FATAL] Failed to clone from repository: " + source.getRepositoryUrl());
			throw new IOException(e);
		}
	}

	private void checkoutCommit(GitSource source, Logger logger, Git git) throws IOException {
		try {
			log.info("Checking out revision: {}", source.getCommitId());

			String branchName = source.getBranchName();

			// Checkout commit if no branch specified
			//   http://stackoverflow.com/a/24893404/2104280
			if(branchName == null) {
				git.checkout()
					.setName(source.getCommitId())
					.call();
			}
			else {
				git.checkout()
					.setName(source.getBranchName())
					.setStartPoint(source.getCommitId())
					.setForce(true)
					.call();
			}
		}
		catch (GitAPIException e) {
			logger.onNextLine("[FATAL] Failed to checkout to specified commit: " + source.getCommitId());
			throw new IOException(e);
		}
	}
	
}
