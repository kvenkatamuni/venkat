package com.paanini.jiffy.storage;

import com.paanini.jiffy.constants.Common;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by rahul on 19/10/15.
 */
public class WrappedFileSystem extends FileSystem {

  private final FileSystem delegate;
  private final String tenantId;
  private final FileMapStrategy mapper;
  private final String root;

  WrappedFileSystem(FileSystem delegate,String root, String tenantId,FileMapStrategy mapper){
    this.delegate = Objects.requireNonNull(delegate);
    this.root = root;
    this.tenantId = Objects.requireNonNull(tenantId);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Override
  public FileSystemProvider provider() {
    return delegate.provider();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public boolean isOpen() {
    return delegate.isOpen();
  }

  @Override
  public boolean isReadOnly() {
    return delegate.isReadOnly();
  }

  @Override
  public String getSeparator() {
    return delegate.getSeparator();
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    throw new UnsupportedOperationException(Common.NOT_IMPLEMENTED_NOW);
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    throw new UnsupportedOperationException(Common.NOT_IMPLEMENTED_NOW);
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    throw new UnsupportedOperationException(Common.NOT_IMPLEMENTED_NOW);
  }

  /**
   * Get a path from FS_ROOT/TENANT/<<MAPPED-LOCATION>>
   * @param first
   * @param more
   * @return
   */
  @Override
  public Path getPath(String first, String... more) {
    String location = joinPath(first, more);
    String[] path = mapper.map(location);
    Path p = getPath(path);
    return this.delegate.getPath(this.root).resolve(this.tenantId).resolve(p);
  }

  private String joinPath(String first, String[] more) {
    return (more != null && more.length > 0) ? first + "/"+ Arrays.asList(more).stream().collect(Collectors.joining("/")) : first;
  }

  Path getPath(String[] path) {
    String firstPath = path[0]; // must have at least one element
    String[] rest = (path.length > 1) ? Arrays.copyOfRange(path, 1, path.length) : null;
    return (rest == null) ? delegate.getPath(firstPath) : delegate.getPath(firstPath,rest);
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    throw new UnsupportedOperationException(Common.NOT_IMPLEMENTED_NOW);
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    throw new UnsupportedOperationException(Common.NOT_IMPLEMENTED_NOW);
  }

  @Override
  public WatchService newWatchService() throws IOException {
    throw new UnsupportedOperationException(Common.NOT_IMPLEMENTED_NOW);
  }
}
