/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.dedupe.file.FileFinder;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * The main class for Dedupe CLI
 * 
 * @author Nicholas Wright
 *
 */
public class DedupeCli {
	private static final Logger LOGGER = LoggerFactory.getLogger(DedupeCli.class);

	public static void main(String[] args) {

		ArgumentParser parser = ArgumentParsers.newArgumentParser("Dedupe CLI").defaultHelp(true)
				.description("Find duplicate files and replace them with links");
		parser.addArgument("dir").nargs("*").help("Directories to walk for files");
		
		Namespace ns = parser.parseArgsOrFail(args);
		
		FileFinder ff = new FileFinder();
		
		
		for(String path : ns.<String> getList("dir")) {
			try {
				Stream<Path> stream = ff.findFiles(Paths.get(path));
				stream.forEach(new Consumer<Path>() {
					@Override
					public void accept(Path t) {
						LOGGER.info(t.toString());
					}
				});
			} catch (IOException e) {
				LOGGER.error("Failed to find files: {}", e.toString());
			}
		}
	}
}
