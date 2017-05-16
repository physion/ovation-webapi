/* {
  "_id": "_design/search",
  "indexes": {
    "all": {
      "index": "function(doc){ ... }"
    }
  }
}
*/

function(doc) {

  if(doc.organization) {
    index("organization", doc.organization, {"store": true})
  }

  if(doc.type) {
    index("type", doc.type, {"store": true});
  }

  if(doc.attributes) {
    for(var k1 in doc.attributes) {
      index(k1, doc.attributes[k1], {"store": true});
      index("default", doc.attributes[k1]);
    }
    index("id", doc._id, {"store": true});
    index("default", doc._id);
  }

  if(doc.type === 'Annotation') {
    for(var k2 in doc.annotation) {
      index(k2, doc.annotation[k2], {"store": true});
      index("default", doc.annotation[k2]);
    }
    index("id", doc.entity, {"store": true});
  }
}
