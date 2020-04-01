
Test suite interchange format
-----------------------------

To share information about a test infrastructure, we use the following JSON format:

```json 
{
   "identifier": "foobar",
   "extraction_date": "ISO8601 date",
   "maven_modules" :
	[
		{ 
		"name": "parentparent",
		"maven_modules" :
		[
			{ 
			"name": "parent",
			"maven_modules" :
			[
				{ 
				"name": "moduleA",
				"junit4_tests": 40,
				"junit5_tests": 10
				},
				{ 
				"name": "moduleB",
				"junit4_tests": 0,
				"junit5_tests": 3
				},
				{ 
				"name": "moduleA",
				"junit4_tests": 9,
				"junit5_tests": 150
				}
			]
		},
		{ 
		"name": "moduleD",
		"junit4_tests": 0,
		"junit5_tests": 3
		},
		{ 
		"name": "moduleD",
		"junit4_tests": 0,
		"junit5_tests": 3
		}
	]
}
```

If the module names cannot be shared, they are anonymized with SHA256 as follows:

```
{ 
"name": "aefed673903edagee56",
"junit4_tests": 0,
"junit5_tests": 3
}

```
