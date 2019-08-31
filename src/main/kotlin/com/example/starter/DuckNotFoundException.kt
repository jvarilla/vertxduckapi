package com.example.starter

import java.lang.Exception

class DuckNotFoundException(status :Int = 404, message :String = "Duck Not Found"): Exception()
