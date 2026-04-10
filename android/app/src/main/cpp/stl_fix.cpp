// Fix for missing STL symbols in glslang library
// This file provides explicit template instantiations for missing symbols

#include <fstream>
#include <sstream>
#include <string>
#include <cstdio>
#include <cstdlib>
#include <cstdarg>

// Provide missing __libcpp_verbose_abort function
// This is required by newer libc++ versions but may not be available in all NDK versions
namespace std {
inline namespace __ndk1 {
__attribute__((visibility("default"), noreturn))
void __libcpp_verbose_abort(char const* format, ...) {
    va_list args;
    va_start(args, format);
    fprintf(stderr, "libc++ fatal error: ");
    vfprintf(stderr, format, args);
    fprintf(stderr, "\n");
    va_end(args);
    abort();
}
}
}

// Explicit template instantiations for missing symbols
// Note: We use the standard std namespace, and the inline namespace __ndk1 is automatically included

namespace std {

// Force instantiation of basic_ofstream
template class basic_ofstream<char, char_traits<char>>;

// Force instantiation of basic_filebuf
template class basic_filebuf<char, char_traits<char>>;

// Force instantiation of basic_stringstream
template class basic_stringstream<char, char_traits<char>, allocator<char>>;

// Force instantiation of basic_ostringstream
template class basic_ostringstream<char, char_traits<char>, allocator<char>>;

// Force instantiation of basic_istringstream
template class basic_istringstream<char, char_traits<char>, allocator<char>>;

// Force instantiation of basic_stringbuf
template class basic_stringbuf<char, char_traits<char>, allocator<char>>;

} // namespace std
