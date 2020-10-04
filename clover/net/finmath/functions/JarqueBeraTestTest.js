var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":33,"id":38604,"methods":[{"el":31,"sc":2,"sl":24}],"name":"JarqueBeraTestTest","sl":22}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_500":{"methods":[{"sl":24}],"name":"test","pass":true,"statements":[{"sl":26},{"sl":28},{"sl":30}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [500], [], [500], [], [500], [], [500], [], [], []]