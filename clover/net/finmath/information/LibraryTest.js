var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":23,"id":39035,"methods":[{"el":14,"sc":2,"sl":8},{"el":22,"sc":2,"sl":16}],"name":"LibraryTest","sl":6}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_102":{"methods":[{"sl":16}],"name":"testVersionString","pass":true,"statements":[{"sl":18},{"sl":20},{"sl":21}]},"test_411":{"methods":[{"sl":8}],"name":"testBuildString","pass":true,"statements":[{"sl":10},{"sl":12},{"sl":13}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [411], [], [411], [], [411], [411], [], [], [102], [], [102], [], [102], [102], [], []]