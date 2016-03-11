/**
 * Created by barry on 9/4/15.
 */
function (doc) {
    if (doc.type && doc.type === "Relation" && doc.user_id) {
        emit([doc.user_id, doc.source_id, doc.rel], null);
        if (doc.inverse_rel) {
            emit([doc.user_id, doc.target_id, doc.inverse_rel], null);
        }

        if (doc.links && doc.links._collaboration_roots) {
            var roots = doc.links._collaboration_roots;
            for (var i = 0; i < roots.length; i++) {
                emit([roots[i], doc.source_id, doc.rel], null);
                if (doc.inverse_rel) {
                    emit([roots[i], doc.target_id, doc.inverse_rel], null);
                }
            }
        }
    }
}
