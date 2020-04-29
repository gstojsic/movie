package org.skunkworks.movie.config

import org.skunkworks.movie.annotation.Movie
import org.springframework.context.annotation.Configuration

@Movie
@Configuration
class MovieConfiguration: Cast()