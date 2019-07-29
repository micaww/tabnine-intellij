package types

class AutocompleteResult(val old_prefix: String, val results: Array<AutocompleteResultEntry>, val user_message: Array<String>)