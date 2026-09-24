// Chat files in the Origin Private File System. Paths are '/'-joined segments that the Kotlin
// side has already resolved and normalised, so no segment is '.', '..' or empty.

function split(path) {
    return path.split('/').filter((segment) => segment.length > 0);
}

// Absent, or a directory where a file was expected (and the reverse).
function isAbsent(error) {
    return error?.name === 'NotFoundError' || error?.name === 'TypeMismatchError';
}

function rethrow(error) {
    throw new Error(`${error?.name ?? 'Error'}: ${error?.message ?? error}`);
}

async function directory(segments, create) {
    let current = await navigator.storage.getDirectory();
    for (const name of segments) {
        current = await current.getDirectoryHandle(name, { create });
    }
    return current;
}

async function fileHandle(path, create) {
    const segments = split(path);
    const parent = await directory(segments.slice(0, -1), create);
    return parent.getFileHandle(segments[segments.length - 1], { create });
}

export async function listFiles(path) {
    try {
        const entries = [];
        const walk = async (dir, prefix) => {
            for await (const [name, handle] of dir.entries()) {
                if (handle.kind === 'file') {
                    entries.push({ path: prefix + name, size: (await handle.getFile()).size });
                } else {
                    await walk(handle, `${prefix}${name}/`);
                }
            }
        };
        await walk(await directory(split(path), false), '');
        return JSON.stringify(entries);
    } catch (error) {
        if (isAbsent(error)) return '[]';
        rethrow(error);
    }
}

export async function fileSize(path) {
    try {
        return (await (await fileHandle(path, false)).getFile()).size;
    } catch (error) {
        if (isAbsent(error)) return null;
        rethrow(error);
    }
}

export async function readText(path) {
    try {
        return await (await (await fileHandle(path, false)).getFile()).text();
    } catch (error) {
        if (isAbsent(error)) return null;
        rethrow(error);
    }
}

export async function writeText(path, content) {
    try {
        const writable = await (await fileHandle(path, true)).createWritable();
        await writable.write(content);
        await writable.close();
    } catch (error) {
        rethrow(error);
    }
}

export async function deleteRecursively(path) {
    const segments = split(path);
    try {
        const parent = await directory(segments.slice(0, -1), false);
        await parent.removeEntry(segments[segments.length - 1], { recursive: true });
    } catch (error) {
        if (isAbsent(error)) return;
        rethrow(error);
    }
}
