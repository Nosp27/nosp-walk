import waitress

from .app_init import app

waitress.serve(app, host="0.0.0.0", port=8080)
