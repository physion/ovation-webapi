/*View mapping function to collate entities by type*/

function(doc) {
    if (doc.type) {
        emit(doc.type, null);
    }
}
