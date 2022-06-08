package tools

import java.util.concurrent.atomic.AtomicReference

class AtomicString(value: String) : AtomicReference<String>(value)