package com.paanini.jiffy.vfs.utils;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * Created by rahul on 22/12/15.
 */
@FunctionalInterface
public interface Call<T> {
  T apply() throws RepositoryException, IOException;
}
