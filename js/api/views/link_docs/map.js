/**
 * Created by barry on 9/4/15.
 */
function (doc) {
    if (doc.type && doc.type === "Relation") {
        emit([doc.source_id, doc.rel], null);
        if (doc.inverse_rel) {
            emit([doc.target_id, doc.inverse_rel], null);
        }
    }
}
