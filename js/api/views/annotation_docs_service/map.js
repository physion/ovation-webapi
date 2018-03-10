/* View for annotation documents by
 * [entity Id, user Id, annotation type]
 */
function(doc) {
    var org = doc.organization;

    if(doc.user && doc.entity && doc.annotation_type) {
        emit([org, doc.entity], null);
        emit([org, doc.entity, doc.annotation_type], null);
        emit([org, doc.entity, doc.user, doc.annotation_type], null);
    }
}
