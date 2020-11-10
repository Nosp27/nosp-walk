import waitress

from . import app_init

waitress.serve(app_init.app, host='0.0.0.0', port=8080)
