package com.code.atlas.web.helper;

import java.util.Locale;

public class FileHelper {

    public static String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public static String languageByExtension(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {

            // JVM
            case "java" -> "java";
            case "kt", "kts" -> "kotlin";
            case "groovy", "gvy", "gy", "gsh" -> "groovy";
            case "scala", "sc" -> "scala";
            case "clj", "cljs", "cljc", "edn" -> "clojure";

            // JavaScript / TypeScript
            case "js", "mjs", "cjs" -> "javascript";
            case "ts", "mts", "cts" -> "typescript";
            case "jsx" -> "jsx";
            case "tsx" -> "tsx";

            // Web
            case "html", "htm" -> "html";
            case "xhtml" -> "xhtml";
            case "css" -> "css";
            case "scss" -> "scss";
            case "sass" -> "sass";
            case "less" -> "less";

            // Backend / scripting
            case "py", "pyw" -> "python";
            case "rb" -> "ruby";
            case "php", "phtml" -> "php";
            case "pl", "pm" -> "perl";
            case "lua" -> "lua";
            case "r" -> "r";
            case "jl" -> "julia";
            case "tcl" -> "tcl";

            // Sistemas
            case "c" -> "c";
            case "h" -> "c";
            case "cpp", "cc", "cxx", "c++" -> "cpp";
            case "hpp", "hh", "hxx" -> "cpp";
            case "rs" -> "rust";
            case "go" -> "go";
            case "zig" -> "zig";
            case "nim" -> "nim";
            case "d" -> "d";

            // Microsoft
            case "cs" -> "csharp";
            case "vb" -> "vbnet";
            case "fs", "fsi", "fsx" -> "fsharp";

            // Funcionales
            case "hs", "lhs" -> "haskell";
            case "elm" -> "elm";
            case "ml", "mli" -> "ocaml";
            case "ex", "exs" -> "elixir";
            case "erl", "hrl" -> "erlang";

            // Mobile
            case "swift" -> "swift";
            case "m" -> "objective-c";
            case "mm" -> "objective-cpp";
            case "dart" -> "dart";

            // Shell
            case "sh", "bash" -> "bash";
            case "zsh" -> "zsh";
            case "fish" -> "fish";
            case "ps1", "psm1", "psd1" -> "powershell";
            case "bat", "cmd" -> "batch";

            // Base de datos
            case "sql" -> "sql";
            case "psql" -> "postgresql";
            case "mysql" -> "mysql";
            case "pgsql" -> "postgresql";
            case "plsql" -> "plsql";

            // Configuración y datos
            case "json" -> "json";
            case "json5" -> "json5";
            case "yaml", "yml" -> "yaml";
            case "toml" -> "toml";
            case "ini", "cfg", "conf", "properties" -> "ini";
            case "env" -> "dotenv";
            case "xml" -> "xml";
            case "xsd", "xsl", "xslt" -> "xml";
            case "csv" -> "csv";
            case "tsv" -> "tsv";

            // Infraestructura / DevOps
            case "tf", "tfvars" -> "terraform";
            case "dockerfile" -> "dockerfile";
            case "hcl" -> "hcl";
            case "nomad" -> "hcl";

            // Build files
            case "gradle" -> "gradle";
            case "mvn", "pom" -> "xml";

            // Documentación
            case "md", "markdown" -> "markdown";
            case "rst" -> "restructuredtext";
            case "adoc", "asciidoc" -> "asciidoc";
            case "tex" -> "latex";

            // Datos científicos
            case "mat" -> "matlab";
            case "mlx" -> "matlab";
            case "nb" -> "mathematica";

            // Otros
            case "graphql", "gql" -> "graphql";
            case "proto" -> "protobuf";
            case "avsc" -> "avro";
            case "thrift" -> "thrift";

            // Ensamblador
            case "asm", "s" -> "assembly";

            default -> "text";
        };
    }
}
