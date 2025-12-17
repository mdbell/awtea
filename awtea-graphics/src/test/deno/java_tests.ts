/**
 * Deno test runner for TeaVM-compiled Java tests
 *
 * This file imports Java tests that have been compiled to JavaScript via TeaVM.
 * The Java code directly calls Deno.test() via JSO wrappers, so tests are
 * automatically registered when main() is called.
 */

import { main } from "../../../build/deno-tests/classes.js";

async function getGitRoot(startPath?: string): Promise<string> {
    let currentPath = startPath ? await Deno.realPath(startPath) : Deno.cwd();

    while (true) {
        try {
            // Check if . git directory exists at current path
            const gitPath = `${currentPath}/.git`;
            const stat = await Deno.stat(gitPath);

            if (stat.isDirectory || stat.isFile) {
                // Found . git (can be a directory or a file for submodules/worktrees)
                return currentPath;
            }
        } catch (_error) {
            // .git doesn't exist at this level, continue
        }

        // Get parent directory
        const parentPath = currentPath.substring(
            0,
            currentPath.lastIndexOf("/"),
        );

        // If we've reached the root without finding .git
        if (parentPath === currentPath || parentPath === "") {
            throw new Error("Not inside a git repository");
        }

        currentPath = parentPath;
    }
}

const gitRoot = await getGitRoot();

const originalFetch = globalThis.fetch;

const fetch_roots = [Deno.cwd(), `${gitRoot}/webapp-common`, gitRoot];

function fetchPolyfill(
    input: string | URL | Request,
    init?: RequestInit,
): Promise<Response> {
    // Extract the URL string
    let urlString: string;
    if (typeof input === "string") {
        urlString = input;
    } else if (input instanceof URL) {
        urlString = input.href;
    } else {
        urlString = input.url;
    }

    // Check if it's a file:// URL or a relative/absolute path
    if (urlString.startsWith("file://") || !urlString.startsWith("http")) {
        // Remove file:// prefix if present
        const filePath = urlString.replace(/^file:\/\//, "");

        for (const path of fetch_roots) {
            try {
                // Read the file
                const data = Deno.readFileSync(`${path}/${filePath}`);

                // Determine content type from file extension
                const ext = filePath.split(".").pop()?.toLowerCase() || "";
                const contentType = getContentType(ext);

                // Create a Response object
                return Promise.resolve(
                    new Response(data, {
                        status: 200,
                        statusText: "OK",
                        headers: {
                            "Content-Type": contentType,
                            "Content-Length": data.length.toString(),
                        },
                    }),
                );
            } catch (_error) {
                // ignored
            }
        }
        // File not found or read error
        return Promise.resolve(
            new Response(null, {
                status: 404,
                statusText: "Not Found",
            }),
        );
    }

    // Fall back to original fetch for HTTP(S) requests
    return originalFetch(input, init);
}

function getContentType(ext: string): string {
    const mimeTypes: Record<string, string> = {
        wasm: "application/wasm",
        js: "application/javascript",
        json: "application/json",
        html: "text/html",
        css: "text/css",
        txt: "text/plain",
        png: "image/png",
        jpg: "image/jpeg",
        jpeg: "image/jpeg",
        gif: "image/gif",
        svg: "image/svg+xml",
    };

    return mimeTypes[ext] || "application/octet-stream";
}

globalThis.fetch = fetchPolyfill;

// Call main() to register all Java tests with Deno
main();
