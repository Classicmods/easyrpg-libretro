#!/bin/bash

# abort on error
set -e

export WORKSPACE=$PWD

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source $SCRIPT_DIR/../shared/import.sh

# Number of CPU
if [ "$(uname)" == "Darwin" ]; then
	nproc=$(getconf _NPROCESSORS_ONLN)
else    
	nproc=$(nproc)
fi

# Use ccache?
# Broken on mingw, ccache is directly put as gcc in the path
if [[ "$(uname)" != MINGW* ]]; then
	test_ccache
fi

if [ ! -f .patches-applied ]; then
	echo "Patching libraries"

        patches_common

        # Fix mpg123
        pushd $MPG123_DIR
        patch -Np1 < $SCRIPT_DIR/../shared/extra/mpg123.patch
        autoreconf -fi
        popd

        # Fix libsndfile
        pushd $LIBSNDFILE_DIR
        patch -Np1 < $SCRIPT_DIR/../shared/extra/libsndfile.patch
        autoreconf -fi
        popd

        # Wildmidi: Vita compatibility
        pushd $WILDMIDI_DIR
        patch -Np1 < $SCRIPT_DIR/wildmidi-libretro.patch
        popd

        # disable libsamplerate examples and tests
        pushd $LIBSAMPLERATE_DIR
        perl -pi -e 's/examples tests//' Makefile.am
        autoreconf -fi
        popd

        # Fix icu build
        # Custom patch because vita newlib provides pthread
        cp -r icu icu-native
        patch -Np0 < $SCRIPT_DIR/icu59-libretro.patch

        cd liblcf
        autoreconf -fi
	cd ..

	touch .patches-applied
fi

cd $WORKSPACE

for x in $LIBPNG_DIR $FREETYPE_DIR $PIXMAN_DIR $LIBOGG_DIR $LIBVORBIS_DIR $MPG123_DIR $LIBSNDFILE_DIR $LIBSAMPLERATE_DIR liblcf; do
	pushd $x
if [ "$(uname)" == "Darwin" ]; then
	touch *
else
	touch --date="`LC_ALL=C date`" *
fi
	popd
done

function install_lib_zlib_windows {
	echo ""
	echo "**** Building zlib (Windows) ****"
	echo ""

	pushd $ZLIB_DIR
	make -fwin32/Makefile.gcc
	mkdir -p ../lib ../include
	cp libz.a ../lib
	cp z*.h ../include
	popd

	echo " -> done"
}

function install_lib_icu_windows {
	echo ""
	echo "**** Building ICU (Windows) ****"
	echo ""

	pushd icu/source

	./runConfigureICU MinGW --enable-static --enable-shared=no --host=i686-pc-mingw32 --prefix=$PLATFORM_PREFIX $ICU_ARGS

	make clean
	make
	make install

	popd
}

echo "Preparing toolchain"

export PLATFORM_PREFIX=$WORKSPACE

export CC="$RETRO_CC"
export CXX="$RETRO_CXX"
export CFLAGS="-O2 -g0 -ffunction-sections -fdata-sections $RETRO_CFLAGS"
export CXXFLAGS="$CFLAGS $RETRO_CXXFLAGS"
export CPPFLAGS="-I$PLATFORM_PREFIX/include -DUSE_LIBRETRO $RETRO_CPPFLAGS"
export LDFLAGS="-L$PLATFORM_PREFIX/lib $RETRO_LDFLAGS"
export MAKEFLAGS="-j${nproc:-2}"
export PKG_CONFIG_PATH=$WORKSPACE/lib/pkgconfig
export PKG_CONFIG_LIBDIR=$PKG_CONFIG_PATH
if [ "$ENABLE_CCACHE" ]; then
	export CC="ccache $RETRO_CC"
	export CXX="ccache $RETRO_CXX"
fi

if [[ "$(uname)" == MINGW* ]]; then
	install_lib_zlib_windows
else
	install_lib_zlib
fi
install_lib $LIBPNG_DIR $LIBPNG_ARGS
install_lib $FREETYPE_DIR $FREETYPE_ARGS --without-harfbuzz
#install_lib $HARFBUZZ_DIR $HARFBUZZ_ARGS
#install_lib $FREETYPE_DIR $FREETYPE_ARGS --with-harfbuzz
install_lib $PIXMAN_DIR $PIXMAN_ARGS
install_lib_cmake $EXPAT_DIR $EXPAT_ARGS
install_lib $LIBOGG_DIR $LIBOGG_ARGS
install_lib $LIBVORBIS_DIR $LIBVORBIS_ARGS
install_lib $MPG123_DIR $MPG123_ARGS
install_lib $LIBSNDFILE_DIR $LIBSNDFILE_ARGS
install_lib_cmake $LIBXMP_LITE_DIR $LIBXMP_LITE_ARGS
install_lib $LIBSAMPLERATE_DIR $LIBSAMPLERATE_ARGS
install_lib_cmake $WILDMIDI_DIR $WILDMIDI_ARGS
#install_lib $OPUS_DIR $OPUS_ARGS
#install_lib $OPUSFILE_DIR $OPUSFILE_ARGS
if [[ "$(uname)" == MINGW* ]]; then
	install_lib_icu_windows
else
	install_lib $ICU_DIR/source $ICU_ARGS
fi
install_lib liblcf

if [[ "$(uname)" == MINGW* ]]; then
	# Fix ICU static library names
	pushd lib
	if [ -e "sicudt.a" ]; then
		mv sicudt.a libicudata.a
	else
		mv libsicudt.a libicudata.a
	fi
	mv libsicuin.a libicui18n.a
	mv libsicutu.a libicutu.a
	mv libsicuuc.a libicuuc.a
	popd
fi
