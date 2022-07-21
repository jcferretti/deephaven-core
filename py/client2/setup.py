#!/usr/bin/env python

import contextlib
import os
import os.path
from os.path import join as pjoin
import re
import shlex
import shutil
import sysconfig

from setuptools import setup, Extension, Distribution

from Cython.Distutils import build_ext as _build_ext
import Cython
from Cython.Build import cythonize

project_name='pydeephaven2'

ext_suffix = sysconfig.get_config_var('EXT_SUFFIX')

@contextlib.contextmanager
def changed_dir(dirname):
    oldcwd = os.getcwd()
    os.chdir(dirname)
    try:
        yield
    finally:
        os.chdir(oldcwd)


class build_ext(_build_ext):
    def build_extensions(self):
        numpy_incl = pkg_resources.resource_filename('numpy', 'core/include')

        self.extensions = [ext for ext in self.extensions
                           if ext.name != '__dummy__']

        for ext in self.extensions:
            if (hasattr(ext, 'include_dirs') and
                    numpy_incl not in ext.include_dirs):
                ext.include_dirs.append(numpy_incl)
        _build_ext.build_extensions(self)

    def run(self):
        self._run_cmake()
        _build_ext.run(self)

    def initialize_options(self):
        self.extra_cmake_args = os.environ.get('CMAKE_OPTIONS', '')
        self.build_type = os.environ.get('BUILD_TYPE',
                                         'release').lower()
        _build_ext.initialize_options(self)

    MODULE_NAMES = ['types']

    def _run_cmake(self):
        # check if build_type is correctly passed / set
        if self.build_type.lower() not in ('release', 'debug'):
            raise ValueError("build_type needs to "
                             "be 'release' or 'debug'")

        # The directory containing this setup.py
        source = os.path.dirname(os.path.abspath(__file__))

        # The staging directory for the module being built
        build_cmd = self.get_finalized_command('build')
        build_temp = pjoin(os.getcwd(), build_cmd.build_temp)
        build_lib = pjoin(os.getcwd(), build_cmd.build_lib)
        saved_cwd = os.getcwd()

        if not os.path.isdir(build_temp):
            self.mkpath(build_temp)

        # Change to the build directory
        with changed_dir(build_temp):
            # Detect if we built elsewhere
            if os.path.isfile('CMakeCache.txt'):
                cachefile = open('CMakeCache.txt', 'r')
                cachedir = re.search('CMAKE_CACHEFILE_DIR:INTERNAL=(.*)',
                                     cachefile.read()).group(1)
                cachefile.close()
                if (cachedir != build_temp):
                    return

            static_lib_option = ''

            cmake_options = [
                static_lib_option,
            ]

            cmake_options.append('-DCMAKE_BUILD_TYPE={0}'
                                 .format(self.build_type.lower()))

            build_tool_args = []
            build_tool_args.append('--')
            if os.environ.get('VERBOSE', '0') == '1':
                cmake_options.append('-DCMAKE_VERBOSE_MAKEFILE=ON')
            if os.environ.get('CMAKE_PREFIX_PATH'):
                cmake_options.append('-DCMAKE_PREFIX_PATH={0}'
                                     .format(os.environ['CMAKE_PREFIX_PATH']))
            if os.environ.get('PARALLEL'):
                build_tool_args.append(
                    '-j{0}'.format(os.environ['PARALLEL']))

            extra_cmake_args = shlex.split(self.extra_cmake_args)

            # Generate the build files
            print("-- Running cmake for " + project_name)
            self.spawn(['cmake'] + extra_cmake_args + cmake_options + [source])
            print("-- Finished cmake for " + project_name)

            print("-- Running cmake --build for " + project_name)
            self.spawn(['cmake', '--build', '.', '--config', self.build_type] +
                       build_tool_args)
            print("-- Finished cmake --build for " + project_name)

            if self.inplace:
                # a bit hacky
                build_lib = saved_cwd

            # Move the libraries to the place expected by the Python build
            try:
                os.makedirs(pjoin(build_lib, project_name))
            except OSError:
                pass

            build_prefix = self.build_type

            if self.bundle_arrow_cpp or self.bundle_arrow_cpp_headers:
                print('Bundling includes: ' + pjoin(build_prefix, 'include'))
                if os.path.exists(pjoin(build_lib, 'pyarrow', 'include')):
                    shutil.rmtree(pjoin(build_lib, 'pyarrow', 'include'))
                shutil.move(pjoin(build_prefix, 'include'),
                            pjoin(build_lib, 'pyarrow'))

            # Move the built C-extension to the place expected by the Python
            # build
            self._found_names = []
            for name in self.MODULE_NAMES:
                built_path = self.get_ext_built(name)
                if not os.path.exists(built_path):
                    print('Did not find {0}'.format(built_path))
                    if self._failure_permitted(name):
                        print('Cython module {0} failure permitted'
                              .format(name))
                        continue
                    raise RuntimeError('pyarrow C-extension failed to build:',
                                       os.path.abspath(built_path))

                # The destination path to move the built C extension to
                ext_path = pjoin(build_lib, self._get_cmake_ext_path(name))
                if os.path.exists(ext_path):
                    os.remove(ext_path)
                self.mkpath(os.path.dirname(ext_path))

                if self.bundle_cython_cpp:
                    self._bundle_cython_cpp(name, build_lib)

                print('Moving built C-extension', built_path,
                      'to build path', ext_path)
                shutil.move(built_path, ext_path)
                self._found_names.append(name)

                if os.path.exists(self.get_ext_built_api_header(name)):
                    shutil.move(self.get_ext_built_api_header(name),
                                pjoin(os.path.dirname(ext_path),
                                      name + '_api.h'))

    def get_ext_generated_cpp_source(self, name):
        return pjoin(name + ".cpp")

    def get_ext_built_api_header(self, name):
        return pjoin(name + "_api.h")

    def get_ext_built(self, name):
        return pjoin(self.build_type, name + ext_suffix)

    def _get_cmake_ext_path(self, name):
        # This is the name of the arrow C-extension
        filename = name + ext_suffix
        return pjoin(self._get_build_dir(), filename)

with open('README.md') as f:
    long_description = f.read()

class BinaryDistribution(Distribution):
    def has_ext_modules(foo):
        return True

install_requires = ()
setup_requires = []
packages = [ project_name ]

setup(
    name=project_name,
    packages=packages,
    zip_safe=False,
    package_data={'pyarrow': ['*.pxd', '*.pyx', 'includes/*.pxd']},
    include_package_data=True,
    distclass=BinaryDistribution,
    # Dummy extension to trigger build_ext
    ext_modules=[Extension('__dummy__', sources=[])],
#    ext_modules=cythonize(Extension(
#        "types.pyx",
#        sources=["../../cpp-client/deephaven/client/src/types.cc"],
#        language="c++"
#    )),
    cmdclass={
        'build_ext': build_ext
    },
    install_requires=install_requires,
    python_requires='>=3.7',
    description='Python client library for Deephaven Community',
    long_description=long_description,
    long_description_content_type='text/markdown',
    classifiers=[
        'Programming Language :: Python :: 3.7',
        'Programming Language :: Python :: 3.8',
        'Programming Language :: Python :: 3.9',
    ],
    license='source vailable',           # TODO
    maintainer='Deephaven',
    maintainer_email='dev@deephaven.io', # TODO
    url='https://deephaven.io/'
)
